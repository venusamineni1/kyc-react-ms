package com.venus.kyc.screening.batch;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/internal/screening/batch")
@Tag(name = "Batch Screening Processing", description = "Endpoints for batch screening operations including XML generation, checksum, encryption, and SFTP upload")
public class BatchScreeningController {

    private final BatchScreeningService batchScreeningService;
    private final MappingConfigRepository mappingConfigRepository;

    public BatchScreeningController(BatchScreeningService batchScreeningService,
            MappingConfigRepository mappingConfigRepository) {
        this.batchScreeningService = batchScreeningService;
        this.mappingConfigRepository = mappingConfigRepository;
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
    public ResponseEntity<String> createBatch(@RequestBody List<com.venus.kyc.screening.batch.model.Client> clients) {
        try {
            Long batchId = batchScreeningService.createBatch(clients);
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
}
