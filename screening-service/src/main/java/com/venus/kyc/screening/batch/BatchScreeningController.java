package com.venus.kyc.screening.batch;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/internal/screening/batch")
@Tag(name = "Batch Screening Processing", description = "Endpoints for batch screening operations including XML generation, checksum, encryption, and SFTP upload")
public class BatchScreeningController {

    private final BatchScreeningService batchScreeningService;
    private final MappingConfigRepository mappingConfigRepository;
    private final BatchScreeningRunRepository batchScreeningRunRepository;
    private final BatchRepository batchRepository;

    public BatchScreeningController(BatchScreeningService batchScreeningService,
            MappingConfigRepository mappingConfigRepository,
            BatchScreeningRunRepository batchScreeningRunRepository,
            BatchRepository batchRepository) {
        this.batchScreeningService = batchScreeningService;
        this.mappingConfigRepository = mappingConfigRepository;
        this.batchScreeningRunRepository = batchScreeningRunRepository;
        this.batchRepository = batchRepository;
    }

    @Operation(summary = "Get screening field mappings", description = "Returns the configured mapping between client fields and screening XML request fields")
    @GetMapping("/mapping")
    public ResponseEntity<List<MappingConfig>> getMapping() {
        return ResponseEntity.ok(mappingConfigRepository.findAll());
    }

    @Operation(summary = "Update screening field mappings", description = "Saves or updates the mapping configuration for batch screening XML generation")
    @PostMapping("/mapping")
    public ResponseEntity<Void> updateMapping(@RequestBody List<MappingConfig> configs) {
        mappingConfigRepository.saveAll(configs);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get batch run history", description = "Returns a list of all previous batch screening runs with their statuses")
    @GetMapping("/history")
    public ResponseEntity<List<BatchRun>> getHistory() {
        return ResponseEntity.ok(batchScreeningService.getBatchHistory());
    }

    @Operation(summary = "Create a new batch", description = "Creates a new batch run record for the given clients without processing")
    @PostMapping("/create")
    public ResponseEntity<String> createBatch(
            @RequestBody List<com.venus.kyc.screening.batch.model.Client> clients,
            @RequestParam(required = false, defaultValue = "MANUAL") String source,
            @RequestParam(required = false, defaultValue = "SYSTEM") String createdBy) {
        try {
            Long batchId = batchScreeningService.createBatch(clients, source, createdBy);
            return ResponseEntity.ok(String.valueOf(batchId));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed: " + e.getMessage());
        }
    }

    @Operation(summary = "Generate XML for batch", description = "Generates the screening XML request file for the specified batch using configured field mappings")
    @PostMapping("/{batchId}/generate-xml")
    public ResponseEntity<String> generateBatchXml(
            @Parameter(description = "Batch run ID") @PathVariable Long batchId) {
        try {
            batchScreeningService.generateBatchXml(batchId);
            return ResponseEntity.ok("XML Generated");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed: " + e.getMessage());
        }
    }

    @Operation(summary = "Generate checksum", description = "Calculates and stores the checksum for batch request file integrity verification")
    @PostMapping("/{batchId}/generate-checksum")
    public ResponseEntity<String> generateBatchChecksum(
            @Parameter(description = "Batch run ID") @PathVariable Long batchId) {
        try {
            batchScreeningService.generateBatchChecksum(batchId);
            return ResponseEntity.ok("Checksum Generated");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed: " + e.getMessage());
        }
    }

    @Operation(summary = "Zip batch files", description = "Compresses the XML and checksum files into a ZIP archive")
    @PostMapping("/{batchId}/zip")
    public ResponseEntity<String> zipBatchFiles(@Parameter(description = "Batch run ID") @PathVariable Long batchId) {
        try {
            batchScreeningService.zipBatchFiles(batchId);
            return ResponseEntity.ok("Files Zipped");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed: " + e.getMessage());
        }
    }

    @Operation(summary = "Encrypt batch file", description = "Encrypts the zipped batch file using GPG encryption for secure transfer")
    @PostMapping("/{batchId}/encrypt")
    public ResponseEntity<String> encryptBatchFile(
            @Parameter(description = "Batch run ID") @PathVariable Long batchId) {
        try {
            batchScreeningService.encryptBatchFile(batchId);
            return ResponseEntity.ok("File Encrypted");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed: " + e.getMessage());
        }
    }

    @Operation(summary = "Upload batch via SFTP", description = "Uploads the encrypted batch file to the SFTP server for processing")
    @PostMapping("/{batchId}/upload")
    public ResponseEntity<String> uploadBatchToSftp(
            @Parameter(description = "Batch run ID") @PathVariable Long batchId) {
        try {
            batchScreeningService.uploadBatchToSftp(batchId);
            return ResponseEntity.ok("File Uploaded");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed: " + e.getMessage());
        }
    }

    @Operation(summary = "Get batch file content", description = "Returns the content of a generated batch file by type (e.g., xml, checksum, zip)")
    @GetMapping("/{batchId}/file-content")
    public ResponseEntity<String> getFileContent(@Parameter(description = "Batch run ID") @PathVariable Long batchId,
            @Parameter(description = "File type: xml, checksum, or zip") @RequestParam String type) {
        try {
            String content = batchScreeningService.getFileContent(batchId, type);
            return ResponseEntity.ok(content);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed: " + e.getMessage());
        }
    }

    @Operation(summary = "Initiate batch (deprecated)", description = "Creates and processes a batch in one step. Use /create and step endpoints instead", deprecated = true)
    @Deprecated
    @PostMapping("/initiate")
    public ResponseEntity<String> initiateBatch(@RequestBody List<com.venus.kyc.screening.batch.model.Client> clients) {
        try {
            Long batchId = batchScreeningService.initiateBatch(clients);
            return ResponseEntity.ok(String.valueOf(batchId));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed: " + e.getMessage());
        }
    }

    @Operation(summary = "Process screening response", description = "Processes a screening response file received from the vendor")
    @PostMapping("/process")
    public ResponseEntity<String> processResponse(
            @Parameter(description = "Response filename to process") @RequestParam String filename) {
        try {
            batchScreeningService.processResponse(filename);
            return ResponseEntity.ok("Processing initiated for: " + filename);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed: " + e.getMessage());
        }
    }

    @Operation(summary = "Get mapping snapshot for batch", description = "Returns the mapping configuration that was used when a specific batch was created")
    @GetMapping("/{batchId}/mapping-snapshot")
    public ResponseEntity<?> getMappingSnapshot(@Parameter(description = "Batch run ID") @PathVariable Long batchId) {
        MappingConfigSnapshot snapshot = batchScreeningService.getMappingSnapshotForBatch(batchId);
        if (snapshot == null) {
            return ResponseEntity.ok(java.util.Map.of("message", "No mapping snapshot linked to this batch"));
        }
        return ResponseEntity.ok(snapshot);
    }

    @Operation(summary = "Get all mapping snapshots", description = "Returns all versioned mapping configuration snapshots for audit purposes")
    @GetMapping("/mapping-snapshots")
    public ResponseEntity<List<MappingConfigSnapshot>> getMappingSnapshots() {
        return ResponseEntity.ok(batchScreeningService.getAllMappingSnapshots());
    }

    @Operation(summary = "Generate test XML", description = "Generates a sample XML payload for a single client using current mapping configuration, for testing purposes")
    @PostMapping("/test-generate")
    public ResponseEntity<String> generateTestXml(@RequestBody com.venus.kyc.screening.batch.model.Client client) {
        try {
            String xml = batchScreeningService.generateTestXml(client);
            return ResponseEntity.ok(xml);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed to generate test XML: " + e.getMessage());
        }
    }

    // ── Mass Screening Run Status ──────────────────────────────────────────────

    @Operation(summary = "Get mass screening run status by runGroupId",
               description = "Returns overall progress of a 700K-client mass screening run and its sub-batches")
    @GetMapping("/runs/{runGroupId}")
    public ResponseEntity<Map<String, Object>> getRunStatus(
            @Parameter(description = "Run group ID (correlationId from .meta.json or UUID from .ack.json)")
            @PathVariable String runGroupId) {

        Optional<BatchScreeningRun> runOpt = batchScreeningRunRepository.findByRunGroupId(runGroupId);
        if (runOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(buildRunStatusResponse(runOpt.get()));
    }

    @Operation(summary = "Get mass screening run status by fileName",
               description = "Looks up the run status by the original CSV filename dropped on SFTP")
    @GetMapping("/runs")
    public ResponseEntity<Map<String, Object>> getRunStatusByFileName(
            @Parameter(description = "Original CSV filename, e.g. DOWNSTREAM_A_clients_20260519143022.csv")
            @RequestParam String fileName) {

        Optional<BatchScreeningRun> runOpt = batchScreeningRunRepository.findByFileName(fileName);
        if (runOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(buildRunStatusResponse(runOpt.get()));
    }

    private Map<String, Object> buildRunStatusResponse(BatchScreeningRun run) {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("runGroupId", run.runGroupId());
        resp.put("fileName", run.fileName());
        resp.put("systemId", run.systemId());
        resp.put("persistClients", run.persistClients());
        resp.put("totalClientCount", run.totalClientCount());
        resp.put("totalBatches", run.totalBatches());
        resp.put("batchesCompleted", run.batchesCompleted());
        resp.put("overallStatus", run.overallStatus());
        resp.put("createdAt", run.createdAt());
        resp.put("completedAt", run.completedAt());

        // Attach sub-batch list
        List<BatchRun> subBatches = batchRepository.findByRunGroupId(run.runGroupId());
        List<Map<String, Object>> batchList = new java.util.ArrayList<>();
        for (int i = 0; i < subBatches.size(); i++) {
            BatchRun b = subBatches.get(i);
            Map<String, Object> bm = new LinkedHashMap<>();
            bm.put("batchId", b.batchID());
            bm.put("batchNumber", i + 1);
            bm.put("batchName", b.batchName());
            bm.put("clientCount", b.clientCount());
            bm.put("status", b.runStatus());
            bm.put("createdAt", b.createdAt());
            bm.put("updatedAt", b.updatedAt());
            batchList.add(bm);
        }
        resp.put("batches", batchList);
        return resp;
    }
}
