package com.venus.kyc.screening;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/internal/screening")
public class ScreeningController {

    private final ScreeningService service;

    public ScreeningController(ScreeningService service) {
        this.service = service;
    }

    @PostMapping("/initiate")
    public ResponseEntity<?> initiateScreening(@RequestBody ScreeningDTOs.ScreeningInternalRequest request) {
        try {
            return ResponseEntity.ok(service.initiateScreening(request));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error initiating screening: " + e.getMessage());
        }
    }

    @GetMapping("/status/{requestId}")
    public ResponseEntity<ScreeningDTOs.ScreeningStatusResponse> getStatus(@PathVariable String requestId) {
        return ResponseEntity.ok(service.checkStatus(requestId));
    }

    @GetMapping("/history/{clientId}")
    public ResponseEntity<List<ScreeningLog>> getHistory(@PathVariable Long clientId) {
        return ResponseEntity.ok(service.getHistory(clientId));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e) {
        return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
    }
}
