package com.venus.kyc.screening;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ScreeningService {

    private final ScreeningRepository repository;
    private final ObjectMapper objectMapper;
    private final ScreeningProvider screeningProvider;

    public ScreeningService(ScreeningRepository repository,
            ObjectMapper objectMapper, ScreeningProvider screeningProvider) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.screeningProvider = screeningProvider;
    }

    public ScreeningDTOs.InitiateScreeningResponse initiateScreening(ScreeningDTOs.ScreeningInternalRequest request) {
        // 1. Construct Backend Request from internal Request
        ScreeningDTOs.ExternalScreeningRequest externalRequest = new ScreeningDTOs.ExternalScreeningRequest(
                request.firstName() + " " + request.lastName(),
                request.dateOfBirth(),
                request.citizenship());

        String requestJson;
        try {
            requestJson = objectMapper.writeValueAsString(externalRequest);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize external request", e);
        }

        // 2. Delegate to Provider
        String externalRequestId = screeningProvider.initiate(externalRequest);

        // 3. Save Log
        ScreeningLog log = new ScreeningLog(null, request.clientId(), requestJson, null, "IN_PROGRESS",
                externalRequestId,
                LocalDateTime.now());
        Long logId = repository.saveLog(log);

        // Initialize empty results as IN_PROGRESS
        saveInitialResults(logId);

        // Note: Audit logging is handled by caller (Viewer) or separate mechanism
        // System.out.println("Initiated screening for client " + request.clientId());

        return new ScreeningDTOs.InitiateScreeningResponse(false, externalRequestId);
    }

    private void saveInitialResults(Long logId) {
        String[] contexts = { "PEP", "ADM", "INT", "SAN" };
        for (String ctx : contexts) {
            repository.saveResult(new ScreeningResult(null, logId, ctx, "IN_PROGRESS", null, null, null));
        }
    }

    public ScreeningDTOs.ScreeningStatusResponse checkStatus(String requestId) {
        ScreeningLog log = repository.findLogByExternalId(requestId);
        if (log == null) {
            throw new RuntimeException("Request ID not found: " + requestId);
        }

        // 1. Check DB first to see if already done?
        List<ScreeningResult> currentResults = repository.findResultsByLogId(log.logID());
        boolean anyInProgress = currentResults.stream().anyMatch(r -> "IN_PROGRESS".equals(r.status()));

        if (!anyInProgress) {
            // Already completed, just return DB results
            List<ScreeningDTOs.ContextResult> dtoResults = currentResults.stream()
                    .map(r -> new ScreeningDTOs.ContextResult(r.contextType(), r.status(), r.alertMessage()))
                    .toList();
            return new ScreeningDTOs.ScreeningStatusResponse(requestId, dtoResults);
        }

        // 2. Poll Provider
        List<ScreeningDTOs.ContextResult> newResults = screeningProvider.checkStatus(requestId);

        if (!newResults.isEmpty()) {
            // Provider returned results, update DB
            repository.deleteResultsByLogId(log.logID()); // Clear old (or in_progress)

            for (ScreeningDTOs.ContextResult res : newResults) {
                repository.saveResult(new ScreeningResult(null, log.logID(), res.contextType(),
                        res.status(),
                        "HIT".equals(res.status()) ? "OPEN" : null,
                        res.alertMessage(),
                        "HIT".equals(res.status()) ? "ALT-" + UUID.randomUUID().toString().substring(0, 5) : null));
            }

            // Update Log Status
            repository.updateLog(log.logID(), "[Provider Response]", "COMPLETED");

            return new ScreeningDTOs.ScreeningStatusResponse(requestId, newResults);
        } else {
            // Still in progress
            List<ScreeningDTOs.ContextResult> dtoResults = currentResults.stream()
                    .map(r -> new ScreeningDTOs.ContextResult(r.contextType(), r.status(), r.alertMessage()))
                    .toList();
            return new ScreeningDTOs.ScreeningStatusResponse(requestId, dtoResults);
        }
    }

    public List<ScreeningLog> getHistory(Long clientId) {
        return repository.findLogsByClientId(clientId);
    }
}
