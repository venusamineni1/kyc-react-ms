package com.venus.kyc.risk.batch;

import com.venus.kyc.risk.batch.model.Client;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/internal/risk/batch")
public class BatchRiskController {

    private final BatchRiskService batchRiskService;
    private final RiskMappingRepository mappingRepository;

    public BatchRiskController(BatchRiskService batchRiskService, RiskMappingRepository mappingRepository) {
        this.batchRiskService = batchRiskService;
        this.mappingRepository = mappingRepository;
    }

    @GetMapping("/mapping")
    public ResponseEntity<List<RiskMapping>> getMapping() {
        return ResponseEntity.ok(mappingRepository.findAll());
    }

    @PostMapping("/mapping")
    public ResponseEntity<Void> updateMapping(@RequestBody List<RiskMapping> configs) {
        mappingRepository.saveAll(configs);
        return ResponseEntity.ok().build();
    }

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

    @PostMapping("/{batchId}/generate-jsonl")
    public ResponseEntity<String> generateBatchJsonl(@PathVariable String batchId) {
        try {
            batchRiskService.generateBatchJsonl(batchId);
            return ResponseEntity.ok("JSONL Generated");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed: " + e.getMessage());
        }
    }

    @PostMapping("/{batchId}/zip")
    public ResponseEntity<String> zipBatch(@PathVariable String batchId) {
        try {
            batchRiskService.zipBatch(batchId);
            return ResponseEntity.ok("Files Zipped");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed: " + e.getMessage());
        }
    }

    @PostMapping("/{batchId}/generate-control")
    public ResponseEntity<String> generateControlFile(@PathVariable String batchId) {
        try {
            batchRiskService.generateControlFile(batchId);
            return ResponseEntity.ok("Control File Generated");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed: " + e.getMessage());
        }
    }

    @PostMapping("/{batchId}/upload")
    public ResponseEntity<String> uploadBatch(@PathVariable String batchId) {
        try {
            batchRiskService.uploadBatch(batchId);
            return ResponseEntity.ok("Uploaded (Mock/Real based on config)");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed: " + e.getMessage());
        }
    }

    @GetMapping("/{batchId}/file-content")
    public ResponseEntity<String> getFileContent(@PathVariable String batchId, @RequestParam String type) {
        try {
            String content = batchRiskService.getFileContent(batchId, type);
            return ResponseEntity.ok(content);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed: " + e.getMessage());
        }
    }
}
