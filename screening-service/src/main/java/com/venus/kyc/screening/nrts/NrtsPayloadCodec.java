package com.venus.kyc.screening.nrts;

import com.venus.kyc.screening.ScreeningDTOs;

import java.util.List;

/**
 * Abstracts the wire format for NRTS submit and status operations.
 *
 * The active implementation is selected via the {@code nrts.format} property:
 *   nrts.format=xml  (default) → NrtsXmlCodec
 *   nrts.format=json           → NrtsJsonCodec (when NRTS migrates its realtime API)
 *
 * NrtsScreeningProvider depends only on this interface, so a format migration
 * is limited to implementing NrtsJsonCodec — no other class needs to change.
 */
public interface NrtsPayloadCodec {

    // ── Result types ─────────────────────────────────────────────────────────

    record SubmitResult(
            boolean anyAlerts,
            Long processId,
            String stat,
            String errorMessage
    ) {}

    record ClientResult(
            Long reqId,
            String clientId,
            String type,
            String name,
            boolean finalFlag,
            List<ScreeningDTOs.AlertContext> alerts
    ) {}

    record StatusResult(Long processId, String overallStat, int noR, List<ClientResult> clients) {
        public boolean isFinalized() {
            return "FINISHED".equalsIgnoreCase(overallStat)
                    || (!clients.isEmpty() && clients.stream().allMatch(ClientResult::finalFlag));
        }
    }

    // ── Exception ─────────────────────────────────────────────────────────────

    class CodecException extends RuntimeException {
        public CodecException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    // ── Contract ──────────────────────────────────────────────────────────────

    /** Serializes one or more NrtsRecords into a NRTS submit request payload. */
    String serializeSubmit(int srcId, List<NrtsRecord> records);

    /** Parses the response body of POST /nrts/submit. */
    SubmitResult parseSubmitResponse(String body);

    /** Parses the response body of GET /nrts/get_status/:processId. */
    StatusResult parseStatusResponse(String body);
}
