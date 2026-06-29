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
    private final ScreeningInteractionRepository interactionRepository;
    private final ObjectMapper objectMapper;
    private final ScreeningProvider screeningProvider;

    public ScreeningService(ScreeningRepository repository,
                             ScreeningInteractionRepository interactionRepository,
                             ObjectMapper objectMapper,
                             ScreeningProvider screeningProvider) {
        this.repository = repository;
        this.interactionRepository = interactionRepository;
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

        // 1. Create the log row up front (before any NRTS call) so the real provider can
        //    attach its NRTS interaction history to it from the very first SUBMIT call.
        ScreeningLog pendingLog = new ScreeningLog(
                null, request.clientId(), requestJson, null, "PENDING",
                "PENDING_" + System.currentTimeMillis(), LocalDateTime.now(), null);
        Long logId = repository.saveLog(pendingLog);

        // 2. Call provider (submit + optional immediate get_status)
        ScreeningDTOs.InitiateScreeningResponse providerResult = screeningProvider.initiate(request, logId);

        // 3. Fill in the outcome on the log row
        String externalId = providerResult.processId() != null
                ? String.valueOf(providerResult.processId())
                : "NO_HIT_" + System.currentTimeMillis();
        String overallStatus = "Hot".equals(providerResult.result()) ? "IN_PROGRESS" : "COMPLETED";
        repository.finalizeInitiatedLog(logId, externalId, overallStatus, providerResult.processId());

        // 4. Persist per-context results
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

        // If now finalized, update our log to COMPLETED.
        // The real NRTS response for this poll was already recorded by NrtsScreeningProvider
        // in ScreeningNrtsInteractions — this just updates the current-state summary.
        if (response.finalized()) {
            ScreeningLog log = repository.findLogByNrtsProcessId(processId);
            if (log != null) {
                repository.updateLog(log.logID(), "COMPLETED");
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

    /** Full append-only NRTS interaction trail (every submit/poll/details/document call) for one screening log. */
    public List<ScreeningNrtsInteraction> getInteractions(Long logId) {
        return interactionRepository.findInteractionsByLogId(logId);
    }

    /** Same interaction trail, looked up by NRTS process ID instead of the internal log ID —
     *  used by viewer-core, which only knows the process ID via its own ScreeningLog copy. */
    public List<ScreeningNrtsInteraction> getInteractionsByProcessId(Long processId) {
        return interactionRepository.findInteractionsByProcessId(processId);
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
