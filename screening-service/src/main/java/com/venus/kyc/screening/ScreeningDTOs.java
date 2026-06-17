package com.venus.kyc.screening;

import java.util.List;

public class ScreeningDTOs {

    // ── Inbound request (from caller → screening-service) ─────────────────────

    public record ScreeningInternalRequest(
            Long clientId,
            String firstName,
            String lastName,
            String dateOfBirth,
            String gender,              // "M" or "F"  → <p:G>
            String citizenship,         // ISO alpha-2  → <p:Cntr>
            String nationality,         // ISO alpha-2  → <p:Nat>  (null = omit element)
            String countryOfResidence,  // ISO alpha-2  → <p:CntrRes> (null = omit element)
            String idType,              // alphanumeric → <p:IdType>
            String idNumber,            // alphanumeric → <p:IdNr>
            String riskRating,          // "H","M","L"  → <p:Risk>
            String comment,             // free text    → <p:Comment>
            String province,            // alphanumeric → <p:Prov>
            Long statusCheckDelayMs     // ms to wait before get_status; 0 = immediate
    ) {}

    // ── Endpoint 1: Initiate response ─────────────────────────────────────────

    public record InitiateScreeningResponse(
            String result,              // "Hot" | "No-Hit"
            Long processId,             // NRTS processId — null on No-Hit
            Long reqId,                 // NRTS reqId for this client — null on No-Hit
            List<String> alertContexts  // e.g. ["PEP","INT"] — empty on No-Hit
    ) {}

    // ── Endpoint 2: Status response ───────────────────────────────────────────

    public record ScreeningStatusResponse(
            String requestId,           // internal externalRequestID / NRTS processId as string
            String overallStatus,       // "In Progress" | "With SIU" | "Finished"
            boolean finalized,          // true = caller should stop polling
            Long reqId,                 // NRTS reqId — pass to Endpoint 3
            List<ContextResult> results
    ) {}

    public record ContextResult(
            String contextType,         // PEP | ADM | INT | SAN
            String status,              // HIT | NO_HIT | IN_PROGRESS
            String alertMessage
    ) {}

    /** Per-alert context from NRTS get_status response */
    public record AlertContext(
            String context,             // PEP | INT | ADM | SAN
            String status,              // e.g. "Under investigation", "False"
            String statusId             // 2-char code, e.g. "00", "01"
    ) {}

    // ── Endpoint 3: Alert details response ────────────────────────────────────

    public record AlertDetailsResponse(
            long requestId,
            String status,              // "FINISHED"
            List<AlertEntry> alerts
    ) {}

    public record AlertEntry(
            String alertId,
            String context,             // derived from alertId (e.g. INT, PEP)
            String alertStatus,
            String lastDecisionDate,
            String lastOperator,
            String lastComments,
            List<HitEntry> hits,        // null for INT alerts
            List<DecisionEntry> decisionHistory,
            List<DocumentRef> alertDocuments
    ) {}

    public record HitEntry(
            String country,
            String city,
            String name,
            String origin,
            String keywords,
            String type                 // O=Other, I=Individual, C=Company
    ) {}

    public record DecisionEntry(
            String date,
            String operator,
            String state,
            String comments,
            DocumentRef document        // null if no attachment for this decision
    ) {}

    public record DocumentRef(
            String filenetId,           // use as documentId in Endpoint 4
            String comments,
            String operator
    ) {}

    // ── Legacy / internal provider DTOs (kept for Mock compatibility) ─────────

    /** Used internally by providers when constructing external requests */
    public record ExternalScreeningRequest(
            String name,
            String dob,
            String country
    ) {}

    public record ExternalScreeningResponse(
            String requestId,
            String status
    ) {}
}
