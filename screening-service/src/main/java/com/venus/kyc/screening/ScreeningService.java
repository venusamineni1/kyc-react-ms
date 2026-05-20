package com.venus.kyc.screening;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ScreeningService {

    private final ScreeningRepository repository;
    private final ObjectMapper objectMapper;
    private final ScreeningProvider screeningProvider;

    public ScreeningService(ScreeningRepository repository,
                             ObjectMapper objectMapper,
                             ScreeningProvider screeningProvider) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.screeningProvider = screeningProvider;
    }

    // ── Endpoint 1: Initiate ─────────────────────────────────────────────────

    /**
     * Submits one client for NRTS screening.
     * If alerts are found, immediately calls get_status (after optional delay)
     * to retrieve context info. Returns Hot/No-Hit + alert contexts.
     */
    public ScreeningDTOs.InitiateScreeningResponse initiateScreening(
            ScreeningDTOs.ScreeningInternalRequest request) {

        String requestJson = serialize(request);

        // 1. Call provider (submit + optional immediate get_status)
        ScreeningDTOs.InitiateScreeningResponse providerResult = screeningProvider.initiate(request);

        // 2. Persist log
        String externalId = providerResult.processId() != null
                ? String.valueOf(providerResult.processId())
                : "NO_HIT_" + System.currentTimeMillis();

        ScreeningLog log = new ScreeningLog(
                null, request.clientId(), requestJson, null,
                "Hot".equals(providerResult.result()) ? "IN_PROGRESS" : "COMPLETED",
                externalId, LocalDateTime.now(), providerResult.processId());
        Long logId = repository.saveLog(log);

        // 3. Persist per-context results
        if ("Hot".equals(providerResult.result()) && providerResult.alertContexts() != null) {
            for (String ctx : providerResult.alertContexts()) {
                repository.saveResult(new ScreeningResult(
                        null, logId, ctx, "HIT", "OPEN", ctx + " alert raised by NRTS",
                        null, providerResult.reqId()));
            }
            // Non-alerted contexts (not returned by initiate) — mark NO_HIT
            List<String> allContexts = List.of("PEP", "ADM", "INT", "SAN");
            for (String ctx : allContexts) {
                if (!providerResult.alertContexts().contains(ctx)) {
                    repository.saveResult(new ScreeningResult(
                            null, logId, ctx, "NO_HIT", null, null, null, null));
                }
            }
        } else {
            // No-Hit: all contexts NO_HIT
            for (String ctx : List.of("PEP", "ADM", "INT", "SAN")) {
                repository.saveResult(new ScreeningResult(
                        null, logId, ctx, "NO_HIT", null, null, null, null));
            }
        }

        return providerResult;
    }

    // ── Endpoint 2: Status ───────────────────────────────────────────────────

    /**
     * Polls NRTS get_status for a given processId.
     * Caller drives the polling schedule. Returns finalized=true when done.
     */
    public ScreeningDTOs.ScreeningStatusResponse checkStatus(long processId) {
        ScreeningDTOs.ScreeningStatusResponse response = screeningProvider.checkStatus(processId);

        // If now finalized, update our log to COMPLETED
        if (response.finalized()) {
            ScreeningLog log = repository.findLogByNrtsProcessId(processId);
            if (log != null) {
                repository.updateLog(log.logID(), "NRTS_FINISHED", "COMPLETED");
                // Update reqId on result rows if we got one back
                if (response.reqId() != null) {
                    repository.updateNrtsReqId(log.logID(), response.reqId());
                }
            }
        }

        return response;
    }

    // ── Endpoint 3: Alert Details ────────────────────────────────────────────

    /**
     * Retrieves full alert decision history from NRTS for a finalized client.
     * Only call after finalized=true and reqId is known from Endpoint 2.
     */
    public ScreeningDTOs.AlertDetailsResponse getAlertDetails(long reqId) {
        return screeningProvider.getAlertDetails(reqId);
    }

    // ── Endpoint 4: Document Download ────────────────────────────────────────

    /**
     * Proxies a Filenet document download from NRTS.
     * documentId is the filenet-id from an alert details response.
     */
    public ResponseEntity<byte[]> getDocument(String documentId) {
        return screeningProvider.getDocument(documentId);
    }

    // ── History (existing) ────────────────────────────────────────────────────

    public List<ScreeningLog> getHistory(Long clientId) {
        return repository.findLogsByClientId(clientId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String serialize(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
