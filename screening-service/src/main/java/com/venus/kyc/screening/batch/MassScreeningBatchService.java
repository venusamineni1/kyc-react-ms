package com.venus.kyc.screening.batch;

import com.venus.kyc.screening.batch.model.Client;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Core async service for processing a 700K-client mass screening CSV.
 *
 * <p>Two modes (controlled by {@code batch.persist-clients}):
 * <ul>
 *   <li><b>Stream-direct</b> (default): parse CSV in chunks of {@code batch.size},
 *       call {@link BatchScreeningService#createBatch} immediately for each chunk.
 *       Lightweight — no staging DB writes.</li>
 *   <li><b>Persist-first</b>: bulk-insert all rows into {@code BatchScreeningStaging},
 *       then page through the staging table to dispatch batches. Crash-resumable.</li>
 * </ul>
 *
 * <p>Called by {@link SftpInputPoller} via {@code @Async} — runs in a separate thread pool.
 */
@Service
public class MassScreeningBatchService {

    private static final Logger log = LoggerFactory.getLogger(MassScreeningBatchService.class);

    private final BatchScreeningService batchScreeningService;
    private final BatchScreeningRunRepository runRepository;
    private final BatchScreeningStagingRepository stagingRepository;
    private final CsvClientMapper csvClientMapper;
    private final FileStorageService fileStorageService;
    private final BatchCompletionNotifier completionNotifier;

    @Value("${batch.size:10000}")
    private int batchSize;

    @Value("${batch.ingestion.chunk.size:5000}")
    private int ingestionChunkSize;

    @Value("${batch.sftp.download.dir:download}")
    private String sftpDownloadDir;

    @Value("${batch.work.dir:batch-work}")
    private String workDir;

    @Value("${batch.keep-temp-files:false}")
    private boolean keepTempFiles;

    public MassScreeningBatchService(BatchScreeningService batchScreeningService,
                                     BatchScreeningRunRepository runRepository,
                                     BatchScreeningStagingRepository stagingRepository,
                                     CsvClientMapper csvClientMapper,
                                     FileStorageService fileStorageService,
                                     BatchCompletionNotifier completionNotifier) {
        this.batchScreeningService = batchScreeningService;
        this.runRepository = runRepository;
        this.stagingRepository = stagingRepository;
        this.csvClientMapper = csvClientMapper;
        this.fileStorageService = fileStorageService;
        this.completionNotifier = completionNotifier;
    }

    /**
     * Entry point called by {@link SftpInputPoller}.
     * Runs fully async — poller returns immediately after this call.
     *
     * @param remoteFilePath  full SFTP path to the CSV file (already moved to processing dir)
     * @param runGroupId      unique correlation ID for this run
     * @param persistClients  if true, bulk-insert to staging before dispatching (Mode B)
     */
    @Async("massScreeningExecutor")
    public void process(String remoteFilePath, String runGroupId, boolean persistClients) {
        log.info("[{}] Starting mass screening processing. file={}, mode={}",
                runGroupId, remoteFilePath, persistClients ? "PERSIST_FIRST" : "STREAM_DIRECT");

        runRepository.updateStatus(runGroupId, "INGESTING");

        File tempCsv = null;
        try {
            // 1. Download CSV from SFTP to a local temp file
            tempCsv = downloadToTemp(remoteFilePath, runGroupId);

            if (persistClients) {
                runPersistFirstMode(tempCsv, runGroupId);
            } else {
                runStreamDirectMode(tempCsv, runGroupId);
            }

        } catch (Exception e) {
            log.error("[{}] Mass screening failed: {}", runGroupId, e.getMessage(), e);
            runRepository.markFailed(runGroupId, e.getMessage());
        } finally {
            if (!keepTempFiles && tempCsv != null && tempCsv.exists()) {
                File tempDir = tempCsv.getParentFile();
                tempCsv.delete();
                if (tempDir != null) tempDir.delete();
                log.debug("[{}] Cleaned up CSV temp dir: {}", runGroupId, tempDir);
            } else if (keepTempFiles && tempCsv != null) {
                log.debug("[{}] Keeping temp CSV: {}", runGroupId, tempCsv.getAbsolutePath());
            }
        }
    }

    // ── Mode A: Stream-Direct ──────────────────────────────────────────────────

    private void runStreamDirectMode(File csvFile, String runGroupId) throws IOException {
        log.info("[{}] Stream-direct mode: reading CSV in {} row pages", runGroupId, batchSize);
        runRepository.updateStatus(runGroupId, "DISPATCHING");

        long totalClients = 0;
        int totalBatches = 0;
        List<Client> currentBatch = new ArrayList<>(batchSize);

        try (Reader reader = Files.newBufferedReader(csvFile.toPath(), StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setIgnoreSurroundingSpaces(true)
                     .setTrim(true)
                     .build()
                     .parse(reader)) {

            for (CSVRecord record : parser) {
                Client client = csvClientMapper.toClient(record);
                currentBatch.add(client);
                totalClients++;

                if (currentBatch.size() >= batchSize) {
                    dispatchBatch(new ArrayList<>(currentBatch), runGroupId, ++totalBatches);
                    currentBatch.clear();
                }
            }

            // Dispatch any remaining rows
            if (!currentBatch.isEmpty()) {
                dispatchBatch(new ArrayList<>(currentBatch), runGroupId, ++totalBatches);
            }
        }

        // Update totals on run record
        runRepository.updateTotals(runGroupId, totalClients, totalBatches);
        log.info("[{}] Stream-direct complete: {} clients in {} batches", runGroupId, totalClients, totalBatches);

        completionNotifier.notifyComplete(runGroupId, totalBatches, totalClients);
    }

    // ── Mode B: Persist-First ─────────────────────────────────────────────────

    private void runPersistFirstMode(File csvFile, String runGroupId) throws IOException {
        log.info("[{}] Persist-first mode: bulk-inserting to staging in {} row chunks", runGroupId, ingestionChunkSize);

        long totalClients = ingestToStaging(csvFile, runGroupId);
        int totalBatches = (int) Math.ceil((double) totalClients / batchSize);
        runRepository.updateTotals(runGroupId, totalClients, totalBatches);
        runRepository.updateStatus(runGroupId, "DISPATCHING");

        log.info("[{}] Staging ingestion complete: {} clients. Dispatching {} batches...", runGroupId, totalClients, totalBatches);
        orchestrateFromStaging(runGroupId, totalBatches, totalClients);
    }

    private long ingestToStaging(File csvFile, String runGroupId) throws IOException {
        long totalInserted = 0;
        List<Client> chunk = new ArrayList<>(ingestionChunkSize);

        try (Reader reader = Files.newBufferedReader(csvFile.toPath(), StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setIgnoreSurroundingSpaces(true)
                     .setTrim(true)
                     .build()
                     .parse(reader)) {

            for (CSVRecord record : parser) {
                chunk.add(csvClientMapper.toClient(record));
                totalInserted++;

                if (chunk.size() >= ingestionChunkSize) {
                    stagingRepository.insertChunk(runGroupId, new ArrayList<>(chunk));
                    log.debug("[{}] Ingested {} rows so far...", runGroupId, totalInserted);
                    chunk.clear();
                }
            }
            if (!chunk.isEmpty()) {
                stagingRepository.insertChunk(runGroupId, chunk);
            }
        }
        return totalInserted;
    }

    private void orchestrateFromStaging(String runGroupId, int totalBatches, long totalClients) {
        long lastSeenId = 0;
        int batchNumber = 0;

        while (true) {
            List<Long> pageIds = stagingRepository.fetchPendingIds(runGroupId, lastSeenId, batchSize);
            if (pageIds.isEmpty()) break;

            lastSeenId = pageIds.get(pageIds.size() - 1);
            List<Client> clients = stagingRepository.fetchPendingPage(runGroupId, pageIds.get(0) - 1, batchSize);
            if (clients.isEmpty()) break;

            dispatchBatch(clients, runGroupId, ++batchNumber);
            stagingRepository.markDispatched(pageIds);
        }

        log.info("[{}] Persist-first orchestration complete: {} clients in {} batches", runGroupId, totalClients, batchNumber);
        // Clean up staging data for completed run
        stagingRepository.deleteByRunGroupId(runGroupId);
        completionNotifier.notifyComplete(runGroupId, totalBatches, totalClients);
    }

    // ── Shared dispatch ────────────────────────────────────────────────────────

    private void dispatchBatch(List<Client> clients, String runGroupId, int batchNumber) {
        log.info("[{}] Dispatching batch {} ({} clients)...", runGroupId, batchNumber, clients.size());
        try {
            // createBatch + full pipeline (XML → checksum → zip → encrypt → SFTP upload)
            Long batchId = batchScreeningService.createBatch(clients, "MASS_SCREENING", runGroupId, batchNumber);
            batchScreeningService.generateBatchXml(batchId);
            batchScreeningService.generateBatchChecksum(batchId);
            batchScreeningService.zipBatchFiles(batchId);
            batchScreeningService.encryptBatchFile(batchId);
            batchScreeningService.uploadBatchToSftp(batchId);

            runRepository.incrementBatchesCompleted(runGroupId);
            log.info("[{}] Batch {} uploaded (batchId={})", runGroupId, batchNumber, batchId);
        } catch (Exception e) {
            log.error("[{}] Batch {} failed: {}", runGroupId, batchNumber, e.getMessage(), e);
            // Continue with remaining batches — individual failures are logged but don't abort the run
        }
    }

    // ── Utility ────────────────────────────────────────────────────────────────

    private File downloadToTemp(String remoteFilePath, String runGroupId) {
        File tempDir = new File(workDir, "mass-screening-" + runGroupId);
        tempDir.mkdirs();
        String fileName = remoteFilePath.substring(remoteFilePath.lastIndexOf('/') + 1);
        File localFile = new File(tempDir, fileName);
        fileStorageService.downloadFile(remoteFilePath, localFile);
        log.info("[{}] Downloaded CSV to temp: {} ({} bytes)", runGroupId, localFile.getAbsolutePath(), localFile.length());
        return localFile;
    }
}
