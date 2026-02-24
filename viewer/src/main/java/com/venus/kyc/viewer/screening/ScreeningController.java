package com.venus.kyc.viewer.screening;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/screening")
@Tag(name = "Viewer Screening", description = "Proxy endpoints for client screening operations including sanctions, PEP checks, and batch screening")
public class ScreeningController {

    private final ScreeningService service;

    public ScreeningController(ScreeningService service) {
        this.service = service;
    }

    @Operation(summary = "Initiate screening", description = "Initiates a sanctions/PEP screening for a specific client")
    @PostMapping("/initiate/{clientId}")
    public ResponseEntity<?> initiateScreening(@Parameter(description = "Client ID") @PathVariable Long clientId) {
        try {
            return ResponseEntity.ok(service.initiateScreening(clientId));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error initiating screening: " + e.getMessage());
        }
    }

    @Operation(summary = "Get screening status", description = "Returns the current status of a screening request")
    @GetMapping("/status/{requestId}")
    public ResponseEntity<ScreeningDTOs.ScreeningStatusResponse> getStatus(
            @Parameter(description = "Screening request ID") @PathVariable String requestId) {
        return ResponseEntity.ok(service.checkStatus(requestId));
    }

    @Operation(summary = "Get screening history", description = "Returns the complete screening history for a specific client")
    @GetMapping("/history/{clientId}")
    public ResponseEntity<List<ScreeningLog>> getHistory(
            @Parameter(description = "Client ID") @PathVariable Long clientId) {
        return ResponseEntity.ok(service.getHistory(clientId));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e) {
        return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
    }

    // Batch Screening Endpoints

    @Operation(summary = "Get batch screening mapping", description = "Returns the configured batch screening field mapping")
    @GetMapping("/batch/mapping")
    public ResponseEntity<List<java.util.Map<String, Object>>> getBatchMapping() {
        return ResponseEntity.ok(service.getBatchMapping());
    }

    @Operation(summary = "Update batch screening mapping", description = "Saves or updates the batch screening field mapping configuration")
    @PostMapping("/batch/mapping")
    public ResponseEntity<Void> updateBatchMapping(@RequestBody List<java.util.Map<String, Object>> configs) {
        service.updateBatchMapping(configs);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Generate test XML", description = "Generates a sample screening XML for a single client using current mapping configuration")
    @PostMapping("/batch/test-generate")
    public ResponseEntity<String> generateBatchTestXml(@RequestBody java.util.Map<String, Object> clientData) {
        return ResponseEntity.ok(service.generateBatchTestXml(clientData));
    }
}
