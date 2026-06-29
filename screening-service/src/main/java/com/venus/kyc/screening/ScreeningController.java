package com.venus.kyc.screening;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/internal/screening", "/api/v1/internal/screening"})
@Tag(name = "Client Screening", description = "NRTS-backed endpoints for initiating and monitoring client sanctions/PEP screenings")
public class ScreeningController {

    private final ScreeningService service;

    public ScreeningController(ScreeningService service) {
        this.service = service;
    }

    // ── Endpoint 1: Initiate ─────────────────────────────────────────────────

    @Operation(
        summary = "Initiate NRTS screening",
        description = """
            Submits a single client to NRTS /nrts/submit.
            If alerts are found (HTTP 202), waits statusCheckDelayMs (0 = immediate)
            then calls NRTS /nrts/get_status once to retrieve initial context info.
            Returns 'Hot' with processId, reqId, and alertContexts, or 'No-Hit'.
            """
    )
    @PostMapping("/initiate")
    public ResponseEntity<?> initiateScreening(
            @RequestBody ScreeningDTOs.ScreeningInternalRequest request) {
        try {
            return ResponseEntity.ok(service.initiateScreening(request));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error initiating screening: " + e.getMessage());
        }
    }

    // ── Endpoint 2: Poll Status ───────────────────────────────────────────────

    @Operation(
        summary = "Poll NRTS screening status",
        description = """
            Calls NRTS /nrts/get_status/:processId for the given processId.
            Returns current investigation status per context, plus a 'finalized' flag.
            Caller is responsible for periodic invocation (e.g. every 6 hours).
            Stop polling when finalized=true. Use the returned reqId for Endpoint 3.
            """
    )
    @GetMapping("/status/{processId}")
    public ResponseEntity<ScreeningDTOs.ScreeningStatusResponse> getStatus(
            @Parameter(description = "NRTS processId returned by /initiate")
            @PathVariable long processId) {
        return ResponseEntity.ok(service.checkStatus(processId));
    }

    // ── Endpoint 3: Alert Details ─────────────────────────────────────────────

    @Operation(
        summary = "Get alert decision history",
        description = """
            Calls NRTS /nrts/get_final_request_details/:requestId.
            Returns full alert history, hits, operator decisions, and Filenet document IDs.
            Only call after finalized=true and client Final=T. Returns 409 if investigation
            is still in progress (NRTS 412 Precondition Failed).
            """
    )
    @GetMapping("/details/{reqId}")
    public ResponseEntity<?> getAlertDetails(
            @Parameter(description = "NRTS reqId for the client, returned by /status")
            @PathVariable long reqId) {
        try {
            return ResponseEntity.ok(service.getAlertDetails(reqId));
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (msg.contains("412")) {
                return ResponseEntity.status(409)
                        .body("Investigation not yet finalized for reqId: " + reqId);
            }
            return ResponseEntity.internalServerError()
                    .body("Error retrieving alert details: " + msg);
        }
    }

    // ── Endpoint 4: Document Download ─────────────────────────────────────────

    @Operation(
        summary = "Download Filenet document",
        description = """
            Calls NRTS /nrts/get_document/:documentId to download an attachment.
            documentId is the filenet-id from an alert details response
            (alertDocuments[].filenetId or decisionHistory[].document.filenetId).
            Returns the raw binary file with original Content-Type and Content-Disposition headers.
            """
    )
    @GetMapping("/document/{documentId}")
    public ResponseEntity<byte[]> getDocument(
            @Parameter(description = "Filenet document ID from alert details response")
            @PathVariable String documentId) {
        return service.getDocument(documentId);
    }

    // ── Existing: History ─────────────────────────────────────────────────────

    @Operation(
        summary = "Get screening history for a client",
        description = "Returns all past screening logs for the given internal client ID"
    )
    @GetMapping("/history/{clientId}")
    public ResponseEntity<List<ScreeningLog>> getHistory(
            @Parameter(description = "Internal client ID") @PathVariable Long clientId) {
        return ResponseEntity.ok(service.getHistory(clientId));
    }

    @Operation(
        summary = "Get full NRTS interaction history for a screening request",
        description = """
            Returns every real request/response exchanged with NRTS for this screening log
            (submit, every status poll, alert details fetch, document fetches) — not just
            the final outcome. Ordered chronologically.
            """
    )
    @GetMapping("/log/{logId}/interactions")
    public ResponseEntity<List<ScreeningNrtsInteraction>> getInteractions(
            @Parameter(description = "ScreeningLogs.LogID") @PathVariable Long logId) {
        return ResponseEntity.ok(service.getInteractions(logId));
    }

    @Operation(
        summary = "Get full NRTS interaction history by NRTS process ID",
        description = """
            Same as /log/{logId}/interactions, but looked up by the NRTS process ID instead —
            for callers (e.g. viewer-core) that only know the process ID, not the internal LogID.
            """
    )
    @GetMapping("/process/{processId}/interactions")
    public ResponseEntity<List<ScreeningNrtsInteraction>> getInteractionsByProcessId(
            @Parameter(description = "NRTS process ID") @PathVariable Long processId) {
        return ResponseEntity.ok(service.getInteractionsByProcessId(processId));
    }

    // ── Exception handler ─────────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e) {
        return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
    }
}
