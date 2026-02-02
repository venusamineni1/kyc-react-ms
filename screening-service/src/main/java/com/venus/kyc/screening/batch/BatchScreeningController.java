package com.venus.kyc.screening.batch;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/screening/batch")
public class BatchScreeningController {

    private final BatchScreeningService batchScreeningService;

    public BatchScreeningController(BatchScreeningService batchScreeningService) {
        this.batchScreeningService = batchScreeningService;
    }

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
}
