package com.venus.kyc.screening.batch;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/internal/screening/batch")
public class BatchScreeningController {

    private final BatchScreeningService batchScreeningService;
    private final MappingConfigRepository mappingConfigRepository;

    public BatchScreeningController(BatchScreeningService batchScreeningService,
            MappingConfigRepository mappingConfigRepository) {
        this.batchScreeningService = batchScreeningService;
        this.mappingConfigRepository = mappingConfigRepository;
    }

    @GetMapping("/mapping")
    public ResponseEntity<List<MappingConfig>> getMapping() {
        return ResponseEntity.ok(mappingConfigRepository.findAll());
    }

    @PostMapping("/mapping")
    public ResponseEntity<Void> updateMapping(@RequestBody List<MappingConfig> configs) {
        mappingConfigRepository.saveAll(configs);
        return ResponseEntity.ok().build();
    }

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

    @PostMapping("/{batchId}/generate-xml")
    public ResponseEntity<String> generateBatchXml(@PathVariable Long batchId) {
        try {
            batchScreeningService.generateBatchXml(batchId);
            return ResponseEntity.ok("XML Generated");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed: " + e.getMessage());
        }
    }

    @PostMapping("/{batchId}/generate-checksum")
    public ResponseEntity<String> generateBatchChecksum(@PathVariable Long batchId) {
        try {
            batchScreeningService.generateBatchChecksum(batchId);
            return ResponseEntity.ok("Checksum Generated");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed: " + e.getMessage());
        }
    }

    @PostMapping("/{batchId}/zip")
    public ResponseEntity<String> zipBatchFiles(@PathVariable Long batchId) {
        try {
            batchScreeningService.zipBatchFiles(batchId);
            return ResponseEntity.ok("Files Zipped");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed: " + e.getMessage());
        }
    }

    @PostMapping("/{batchId}/encrypt")
    public ResponseEntity<String> encryptBatchFile(@PathVariable Long batchId) {
        try {
            batchScreeningService.encryptBatchFile(batchId);
            return ResponseEntity.ok("File Encrypted");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed: " + e.getMessage());
        }
    }

    @PostMapping("/{batchId}/upload")
    public ResponseEntity<String> uploadBatchToSftp(@PathVariable Long batchId) {
        try {
            batchScreeningService.uploadBatchToSftp(batchId);
            return ResponseEntity.ok("File Uploaded");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed: " + e.getMessage());
        }
    }

    @GetMapping("/{batchId}/file-content")
    public ResponseEntity<String> getFileContent(@PathVariable Long batchId, @RequestParam String type) {
        try {
            String content = batchScreeningService.getFileContent(batchId, type);
            return ResponseEntity.ok(content);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed: " + e.getMessage());
        }
    }

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

    @PostMapping("/process")
    public ResponseEntity<String> processResponse(@RequestParam String filename) {
        try {
            batchScreeningService.processResponse(filename);
            return ResponseEntity.ok("Processing initiated for: " + filename);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed: " + e.getMessage());
        }
    }

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
