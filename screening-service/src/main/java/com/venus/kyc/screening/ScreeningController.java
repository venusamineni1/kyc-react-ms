package com.venus.kyc.screening;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/internal/screening")
@Tag(name = "Client Screening", description = "Endpoints for initiating and monitoring client sanctions/PEP screenings")
public class ScreeningController {

    private final ScreeningService service;

    public ScreeningController(ScreeningService service) {
        this.service = service;
    }

    @Operation(summary = "Initiate client screening", description = "Submits a screening request for a client to check against sanctions, PEP, and adverse media lists")
    @PostMapping("/initiate")
    public ResponseEntity<?> initiateScreening(@RequestBody ScreeningDTOs.ScreeningInternalRequest request) {
        try {
            return ResponseEntity.ok(service.initiateScreening(request));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error initiating screening: " + e.getMessage());
        }
    }

    @Operation(summary = "Get screening status", description = "Returns the current status of a screening request by its unique request ID")
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
}
