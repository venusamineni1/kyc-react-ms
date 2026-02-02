package com.venus.kyc.viewer.screening;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/screening")
public class ScreeningController {

    private final ScreeningService service;

    public ScreeningController(ScreeningService service) {
        this.service = service;
    }

    @PostMapping("/initiate/{clientId}")
    public ResponseEntity<?> initiateScreening(@PathVariable Long clientId) {
        try {
            return ResponseEntity.ok(service.initiateScreening(clientId));
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
