package com.venus.kyc.risk.batch;

import com.venus.kyc.risk.batch.model.Client;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.List;

@RestController
@RequestMapping("/api/risk/batch")
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
            File resultFile = batchRiskService.initiateBatch(clients);
            return ResponseEntity.ok("Batch file generated: " + resultFile.getAbsolutePath());
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
}
