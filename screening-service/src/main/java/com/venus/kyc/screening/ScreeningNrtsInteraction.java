package com.venus.kyc.screening;

import java.time.LocalDateTime;

/**
 * One real request/response exchange with NRTS (submit, status poll, alert details fetch, or
 * document fetch). Append-only — every interaction for a screening request gets its own row,
 * unlike ScreeningLogs which only tracks the current/latest state.
 */
public record ScreeningNrtsInteraction(
        Long interactionID,
        Long screeningLogID,
        Long clientID,
        String externalRequestID,
        Long nrtsProcessId,
        Long nrtsReqId,
        String interactionType, // SUBMIT | STATUS_POLL | ALERT_DETAILS | DOCUMENT_FETCH
        Integer httpStatus,
        String requestPayload,
        String responsePayload,
        Boolean isFinal,
        LocalDateTime createdAt
) {}
