package com.venus.kyc.risk.batch;

import com.venus.kyc.risk.batch.model.Client;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/internal/risk/batch")
@Tag(name = "Batch Risk Processing", description = "Endpoints for batch risk assessment including JSONL generation, file compression, and SFTP upload")
public class BatchRiskController {

    private final BatchRiskService batchRiskService;
    private final RiskMappingRepository mappingRepository;

    public BatchRiskController(BatchRiskService batchRiskService, RiskMappingRepository mappingRepository) {
        this.batchRiskService = batchRiskService;
        this.mappingRepository = mappingRepository;
    }

    @Operation(summary = "Get risk field mappings", description = "Returns the configured mapping between client fields and risk engine input fields")
    @GetMapping("/mapping")
    public ResponseEntity<List<RiskMapping>> getMapping() {
        return ResponseEntity.ok(mappingRepository.findAll());
    }

    @Operation(summary = "Update risk field mappings", description = "Saves or updates the mapping configuration for risk batch processing")
    @PostMapping("/mapping")
    public ResponseEntity<Void> updateMapping(@RequestBody List<RiskMapping> configs) {
        mappingRepository.saveAll(configs);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get batch run history", description = "Returns a list of all previous batch risk assessment runs with their statuses")
    @GetMapping("/history")
    public ResponseEntity<List<BatchRun>> getHistory() {
        return ResponseEntity.ok(batchRiskService.getBatchHistory());
    }

    @Operation(summary = "Initiate batch risk assessment", description = "Creates and processes a batch risk assessment for the given list of clients in one step")
    @PostMapping("/initiate")
    public ResponseEntity<String> initiateBatch(@RequestBody List<Client> clients) {
        try {
            String batchName = batchRiskService.initiateBatch(clients);
            return ResponseEntity.ok("Batch Initiated: " + batchName);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed: " + e.getMessage());
        }
    }

    @Operation(summary = "Generate test JSON", description = "Generates a sample JSONL payload for a single client using current mapping configuration, for testing purposes")
    @PostMapping("/test-generate")
    public ResponseEntity<String> generateTestJson(@RequestBody Client client) {
        try {
            String json = batchRiskService.generateTestJson(client);
            return ResponseEntity.ok(json);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed: " + e.getMessage());
        }
    }

    @Operation(summary = "Create a new batch", description = "Creates a new batch run record for the given clients without processing. Returns the batch ID for step-by-step processing")
    @PostMapping("/create")
    public ResponseEntity<String> createBatch(@RequestBody List<Client> clients) {
        try {
            String batchId = batchRiskService.createBatch(clients);
            return ResponseEntity.ok(batchId);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed: " + e.getMessage());
        }
    }

    @Operation(summary = "Generate JSONL for batch", description = "Generates the JSONL request file for the specified batch using configured field mappings")
    @PostMapping("/{batchId}/generate-jsonl")
    public ResponseEntity<String> generateBatchJsonl(
            @Parameter(description = "Batch run ID") @PathVariable String batchId) {
        try {
            batchRiskService.generateBatchJsonl(batchId);
            return ResponseEntity.ok("JSONL Generated");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed: " + e.getMessage());
        }
    }

    @Operation(summary = "Zip batch files", description = "Compresses the generated batch files into a ZIP archive")
    @PostMapping("/{batchId}/zip")
    public ResponseEntity<String> zipBatch(@Parameter(description = "Batch run ID") @PathVariable String batchId) {
        try {
            batchRiskService.zipBatch(batchId);
            return ResponseEntity.ok("Files Zipped");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed: " + e.getMessage());
        }
    }

    @Operation(summary = "Generate control file", description = "Creates the control/manifest file for the batch submission")
    @PostMapping("/{batchId}/generate-control")
    public ResponseEntity<String> generateControlFile(
            @Parameter(description = "Batch run ID") @PathVariable String batchId) {
        try {
            batchRiskService.generateControlFile(batchId);
            return ResponseEntity.ok("Control File Generated");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed: " + e.getMessage());
        }
    }

    @Operation(summary = "Upload batch via SFTP", description = "Uploads the compressed batch file to the SFTP server for processing")
    @PostMapping("/{batchId}/upload")
    public ResponseEntity<String> uploadBatch(@Parameter(description = "Batch run ID") @PathVariable String batchId) {
        try {
            batchRiskService.uploadBatch(batchId);
            return ResponseEntity.ok("Uploaded (Mock/Real based on config)");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed: " + e.getMessage());
        }
    }

    @Operation(summary = "Get batch file content", description = "Returns the content of a generated batch file by type (e.g., jsonl, zip, control)")
    @GetMapping("/{batchId}/file-content")
    public ResponseEntity<String> getFileContent(@Parameter(description = "Batch run ID") @PathVariable String batchId,
            @Parameter(description = "File type: jsonl, zip, or control") @RequestParam String type) {
        try {
            String content = batchRiskService.getFileContent(batchId, type);
            return ResponseEntity.ok(content);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed: " + e.getMessage());
        }
    }
}
