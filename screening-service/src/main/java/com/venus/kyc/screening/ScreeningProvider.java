package com.venus.kyc.screening;

import org.springframework.http.ResponseEntity;

import java.util.List;

public interface ScreeningProvider {

    /**
     * Endpoint 1 — Submit a client for screening.
     * Internally calls NRTS /nrts/submit, then if alerts found waits
     * statusCheckDelayMs before calling /nrts/get_status once to get context info.
     *
     * @param request the internal screening request
     * @param screeningLogId the ScreeningLogs row already created for this request, so the
     *                        real implementation can attach its NRTS interaction history to it
     * @return InitiateScreeningResponse with result ("Hot"/"No-Hit"), processId, reqId, alertContexts
     */
    ScreeningDTOs.InitiateScreeningResponse initiate(ScreeningDTOs.ScreeningInternalRequest request, Long screeningLogId);

    /**
     * Endpoint 2 — Poll current investigation status for a process.
     * Calls NRTS /nrts/get_status/:processId.
     * Caller is responsible for periodic invocation until finalized=true.
     *
     * @param processId the NRTS processId (numeric)
     * @return ScreeningStatusResponse with finalized flag and per-client alert contexts
     */
    ScreeningDTOs.ScreeningStatusResponse checkStatus(long processId);

    /**
     * Endpoint 3 — Retrieve alert decision history for a finalized client.
     * Calls NRTS /nrts/get_final_request_details/:requestId.
     * Should only be called after Final=T for the client.
     *
     * @param reqId the NRTS ReqId for the client
     * @return AlertDetailsResponse with full alert history, hits, decisions, filenet-ids
     */
    ScreeningDTOs.AlertDetailsResponse getAlertDetails(long reqId);

    /**
     * Endpoint 4 — Download an attachment document from Filenet.
     * Calls NRTS /nrts/get_document/:documentId.
     *
     * @param documentId the filenet-id from an alert details response
     * @return ResponseEntity with binary file bytes and Content-Type/Content-Disposition headers
     */
    ResponseEntity<byte[]> getDocument(String documentId);
}
