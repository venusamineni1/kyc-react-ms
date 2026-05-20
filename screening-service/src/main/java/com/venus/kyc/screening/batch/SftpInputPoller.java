package com.venus.kyc.screening.batch;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Polls the SFTP input directory every {@code batch.poller.interval} ms for new
 * {@code *.csv} client-list files dropped by downstream systems.
 *
 * <h3>File lifecycle</h3>
 * <pre>
 *   batch/input/SYSTEM_clients_TIMESTAMP.csv          ← downstream drops here
 *   batch/input/SYSTEM_clients_TIMESTAMP.meta.json    ← optional companion
 *        │
 *        ▼ (detected by this poller)
 *   batch/input/processing/SYSTEM_clients_TIMESTAMP.csv   ← moved atomically
 *        │
 *        ▼ (MassScreeningBatchService picks up)
 *   batch/output/ack/SYSTEM_clients_TIMESTAMP.ack.json    ← written immediately
 * </pre>
 *
 * <h3>Run-group ID resolution</h3>
 * <ol>
 *   <li>If {@code .meta.json} contains {@code correlationId} → use it as runGroupId.</li>
 *   <li>Otherwise → generate a UUID and write an {@code .ack.json} so downstream can discover it.</li>
 *   <li>Downstream can also query by filename: {@code GET /api/internal/screening/batch/runs?fileName=...}</li>
 * </ol>
 */
@Component
public class SftpInputPoller {

    private static final Logger log = LoggerFactory.getLogger(SftpInputPoller.class);

    private final FileStorageService fileStorageService;
    private final MassScreeningBatchService massScreeningBatchService;
    private final BatchScreeningRunRepository runRepository;
    private final ObjectMapper objectMapper;

    @Value("${batch.input.sftp.dir:batch/input}")
    private String inputDir;

    @Value("${batch.input.processing.dir:batch/input/processing}")
    private String processingDir;

    @Value("${batch.input.failed.dir:batch/input/failed}")
    private String failedDir;

    @Value("${batch.input.ack.dir:batch/output/ack}")
    private String ackDir;

    @Value("${batch.persist-clients:false}")
    private boolean persistClients;

    public SftpInputPoller(FileStorageService fileStorageService,
                           MassScreeningBatchService massScreeningBatchService,
                           BatchScreeningRunRepository runRepository,
                           ObjectMapper objectMapper) {
        this.fileStorageService = fileStorageService;
        this.massScreeningBatchService = massScreeningBatchService;
        this.runRepository = runRepository;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${batch.poller.interval:30000}")
    public void pollForNewClientFiles() {
        try {
            List<String> files = fileStorageService.listFiles(inputDir);
            for (String fileName : files) {
                if (fileName.endsWith(".csv")) {
                    processFile(fileName);
                }
            }
        } catch (Exception e) {
            log.error("SftpInputPoller error: {}", e.getMessage(), e);
        }
    }

    private void processFile(String csvFileName) {
        String csvRemotePath = inputDir + "/" + csvFileName;
        String metaRemotePath = inputDir + "/" + csvFileName.replace(".csv", ".meta.json");
        log.info("Detected new client CSV: {}", csvFileName);

        try {
            // 1. Read optional companion .meta.json
            Meta meta = readMeta(metaRemotePath, csvFileName);

            // 2. Resolve runGroupId
            String runGroupId = (meta.correlationId != null && !meta.correlationId.isBlank())
                    ? meta.correlationId
                    : UUID.randomUUID().toString();

            boolean persist = meta.persistClients != null ? meta.persistClients : persistClients;

            // 3. Create tracking record in DB
            BatchScreeningRun run = new BatchScreeningRun(
                    null, runGroupId, csvFileName, meta.systemId,
                    null, null, 0, "DETECTED",
                    persist, meta.callbackWebhookUrl, null,
                    LocalDateTime.now(), null
            );
            runRepository.save(run);

            // 4. Move CSV to processing dir (atomic rename)
            fileStorageService.mkdirs(processingDir);
            String processingPath = processingDir + "/" + csvFileName;
            fileStorageService.renameFile(csvRemotePath, processingPath);

            // 5. Delete companion .meta.json if present
            if (fileStorageService.exists(metaRemotePath)) {
                fileStorageService.deleteFile(metaRemotePath);
            }

            // 6. Write .ack.json so downstream can discover the runGroupId
            writeAck(csvFileName, runGroupId);

            log.info("[{}] Enqueuing mass screening for file={}, mode={}, systemId={}",
                    runGroupId, csvFileName, persist ? "PERSIST_FIRST" : "STREAM_DIRECT", meta.systemId);

            // 7. Fire async processing (returns immediately)
            massScreeningBatchService.process(processingPath, runGroupId, persist);

        } catch (Exception e) {
            log.error("Failed to initiate mass screening for {}: {}", csvFileName, e.getMessage(), e);
            try {
                // Move to failed dir for manual inspection
                fileStorageService.mkdirs(failedDir);
                fileStorageService.renameFile(csvRemotePath, failedDir + "/" + csvFileName);
            } catch (Exception moveEx) {
                log.error("Could not move failed file {} to failed dir: {}", csvFileName, moveEx.getMessage());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Meta readMeta(String metaRemotePath, String csvFileName) {
        Meta meta = new Meta();
        // Derive systemId from filename: DOWNSTREAM_A_clients_20260519.csv → DOWNSTREAM_A
        String namePart = csvFileName.replace(".csv", "");
        int clientsIdx = namePart.lastIndexOf("_clients_");
        if (clientsIdx > 0) {
            meta.systemId = namePart.substring(0, clientsIdx);
        }

        try {
            String json = fileStorageService.readString(metaRemotePath);
            if (json != null && !json.isBlank()) {
                Map<String, Object> map = objectMapper.readValue(json, Map.class);
                meta.correlationId = (String) map.get("correlationId");
                if (map.containsKey("systemId")) meta.systemId = (String) map.get("systemId");
                meta.callbackWebhookUrl = (String) map.get("callbackWebhookUrl");
                if (map.get("persistClients") instanceof Boolean b) meta.persistClients = b;
            }
        } catch (Exception e) {
            log.debug("No valid .meta.json for {}: {}", csvFileName, e.getMessage());
        }
        return meta;
    }

    private void writeAck(String csvFileName, String runGroupId) {
        try {
            fileStorageService.mkdirs(ackDir);
            String ackFileName = csvFileName.replace(".csv", ".ack.json");
            Map<String, Object> ack = Map.of(
                    "fileName", csvFileName,
                    "runGroupId", runGroupId,
                    "status", "ACCEPTED",
                    "acceptedAt", LocalDateTime.now().toString(),
                    "statusPollingUrl", "/api/internal/screening/batch/runs/" + runGroupId
            );
            fileStorageService.writeString(objectMapper.writeValueAsString(ack), ackDir + "/" + ackFileName);
            log.debug("[{}] ACK written: {}/{}", runGroupId, ackDir, ackFileName);
        } catch (Exception e) {
            log.warn("[{}] Could not write .ack.json: {}", runGroupId, e.getMessage());
        }
    }

    /** Parsed from optional .meta.json companion file. */
    private static class Meta {
        String correlationId;
        String systemId;
        String callbackWebhookUrl;
        Boolean persistClients;
    }
}
