package com.venus.kyc.screening.nrts;

import com.venus.kyc.screening.ScreeningDTOs;
import com.venus.kyc.screening.ScreeningLog;
import com.venus.kyc.screening.ScreeningProvider;
import com.venus.kyc.screening.ScreeningRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Real NRTS implementation of ScreeningProvider.
 * Active when nrts.mock=false.
 *
 * Depends on NrtsPayloadCodec (not a concrete XML/JSON class) so that a
 * format change from XML to JSON is isolated to NrtsJsonCodec only.
 *
 * Every real NRTS call is recorded as an append-only row in ScreeningNrtsInteractions
 * (via ScreeningInteractionRepository, which encrypts the payload columns) — this is the
 * full audit trail of what NRTS actually said, at every step, not just the final outcome.
 */
@Component
@ConditionalOnProperty(name = "nrts.mock", havingValue = "false")
public class NrtsScreeningProvider implements ScreeningProvider {

    private static final Logger log = LoggerFactory.getLogger(NrtsScreeningProvider.class);

    private final NrtsConfig config;
    private final NrtsHttpClient httpClient;
    private final NrtsPayloadCodec codec;
    private final NrtsJsonParser jsonParser;
    private final NrtsInteractionRecorder interactionRecorder;
    private final ScreeningRepository screeningRepository;

    public NrtsScreeningProvider(NrtsConfig config, NrtsHttpClient httpClient,
                                  NrtsPayloadCodec codec, NrtsJsonParser jsonParser,
                                  NrtsInteractionRecorder interactionRecorder,
                                  ScreeningRepository screeningRepository) {
        this.config = config;
        this.httpClient = httpClient;
        this.codec = codec;
        this.jsonParser = jsonParser;
        this.interactionRecorder = interactionRecorder;
        this.screeningRepository = screeningRepository;
    }

    // ── Endpoint 1: Initiate ─────────────────────────────────────────────────

    @Override
    public ScreeningDTOs.InitiateScreeningResponse initiate(ScreeningDTOs.ScreeningInternalRequest request, Long screeningLogId) {
        // 1. Build format-agnostic record and serialize
        NrtsRecord record = toNrtsRecord(request);
        String payload = codec.serializeSubmit(config.srcId(), List.of(record));
        log.debug("NRTS submit payload:\n{}", payload);

        // 2. Submit to NRTS
        NrtsHttpClient.NrtsRawResponse submitResponse = httpClient.submit(payload);
        log.info("NRTS submit HTTP {}", submitResponse.httpStatus());
        interactionRecorder.record(screeningLogId, request.clientId(), null, null, null,
                "SUBMIT", submitResponse.httpStatus(), payload, submitResponse.body(), false);

        NrtsPayloadCodec.SubmitResult submitResult = codec.parseSubmitResponse(submitResponse.body());

        // 3. No alerts → No-Hit
        if (!submitResult.anyAlerts()) {
            log.info("NRTS submit: No alerts for client {}", request.clientId());
            return new ScreeningDTOs.InitiateScreeningResponse("No-Hit", null, null, Collections.emptyList());
        }

        // 4. Alerts found → wait configured delay, then call get_status once
        long delayMs = request.statusCheckDelayMs() != null
                ? request.statusCheckDelayMs()
                : config.statusCheckDelayMs();

        if (delayMs > 0) {
            log.info("NRTS: alerts found, waiting {}ms before first get_status call", delayMs);
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        Long processId = submitResult.processId();
        NrtsHttpClient.NrtsRawResponse statusResponse = httpClient.getStatus(processId);
        NrtsPayloadCodec.StatusResult statusResult = codec.parseStatusResponse(statusResponse.body());

        NrtsPayloadCodec.ClientResult client = statusResult.clients().isEmpty()
                ? null : statusResult.clients().get(0);

        List<String> alertContexts = client == null ? Collections.emptyList()
                : client.alerts().stream()
                        .map(ScreeningDTOs.AlertContext::context)
                        .collect(Collectors.toList());

        Long reqId = client != null ? client.reqId() : null;

        interactionRecorder.record(screeningLogId, request.clientId(), null, processId, reqId,
                "STATUS_POLL", statusResponse.httpStatus(), "GET /nrts/get_status/" + processId,
                statusResponse.body(), statusResult.isFinalized());

        log.info("NRTS initiate: Hot — processId={}, reqId={}, contexts={}", processId, reqId, alertContexts);
        return new ScreeningDTOs.InitiateScreeningResponse("Hot", processId, reqId, alertContexts);
    }

    // ── Endpoint 2: Status ───────────────────────────────────────────────────

    @Override
    public ScreeningDTOs.ScreeningStatusResponse checkStatus(long processId) {
        NrtsHttpClient.NrtsRawResponse statusResponse = httpClient.getStatus(processId);
        NrtsPayloadCodec.StatusResult result = codec.parseStatusResponse(statusResponse.body());

        NrtsPayloadCodec.ClientResult client = result.clients().isEmpty()
                ? null : result.clients().get(0);

        List<ScreeningDTOs.ContextResult> contextResults = client == null
                ? Collections.emptyList()
                : client.alerts().stream()
                        .map(a -> new ScreeningDTOs.ContextResult(a.context(), a.status(), buildAlertMessage(a)))
                        .collect(Collectors.toList());

        Long reqId = client != null ? client.reqId() : null;

        // Every poll is recorded, not just the final one — the log row already exists by now
        // (created during initiate()), so it can be resolved by NrtsProcessId.
        ScreeningLog existingLog = screeningRepository.findLogByNrtsProcessId(processId);
        interactionRecorder.record(existingLog != null ? existingLog.logID() : null,
                existingLog != null ? existingLog.clientID() : null,
                null, processId, reqId, "STATUS_POLL", statusResponse.httpStatus(),
                "GET /nrts/get_status/" + processId, statusResponse.body(), result.isFinalized());

        return new ScreeningDTOs.ScreeningStatusResponse(
                String.valueOf(processId),
                result.overallStat(),
                result.isFinalized(),
                reqId,
                contextResults
        );
    }

    // ── Endpoint 3: Alert Details ────────────────────────────────────────────

    @Override
    public ScreeningDTOs.AlertDetailsResponse getAlertDetails(long reqId) {
        NrtsHttpClient.NrtsRawResponse raw = httpClient.getDetails(reqId);

        // Resolve the owning log via the per-context result rows (1 client = 1 ReqId).
        Long logId = screeningRepository.findLogIdByNrtsReqId(reqId);
        interactionRecorder.record(logId, null, null, null, reqId, "ALERT_DETAILS", raw.httpStatus(),
                "GET /nrts/get_final_request_details/" + reqId, raw.body(), true);

        return jsonParser.parseDetailsResponse(reqId, raw.body());
    }

    // ── Endpoint 4: Document Download ────────────────────────────────────────

    @Override
    public ResponseEntity<byte[]> getDocument(String documentId) {
        ResponseEntity<byte[]> response = httpClient.getDocument(documentId);

        // No reliable correlation key back to a ScreeningLogID from a Filenet documentId alone,
        // so this is recorded unlinked (ScreeningLogID null) — still queryable by documentId/time.
        // The binary body itself isn't stored here; document-service already has a BLOB-versioned
        // pattern if full binary audit is needed.
        interactionRecorder.record(null, null, null, null, null, "DOCUMENT_FETCH",
                response.getStatusCode().value(), "GET /nrts/get_document/" + documentId,
                "[binary, " + (response.getBody() != null ? response.getBody().length : 0) + " bytes]", true);

        return response;
    }

    /**
     * NRTS get_status only returns a numeric StatusId per alert, not free-text — synthesize a
     * readable message from it instead of surfacing the bare code to analysts (e.g. "17").
     */
    private String buildAlertMessage(ScreeningDTOs.AlertContext alert) {
        if (!"HIT".equalsIgnoreCase(alert.status()) || alert.statusId() == null) return null;
        return alert.context() + " match flagged by NRTS (status code " + alert.statusId() + ")";
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private NrtsRecord toNrtsRecord(ScreeningDTOs.ScreeningInternalRequest req) {
        return new NrtsRecord(
                req.clientId() != null ? String.valueOf(req.clientId()) : null,
                "I",
                req.firstName(),
                req.lastName(),
                req.dateOfBirth(),
                req.gender(),
                req.citizenship(),
                req.nationality(),
                req.countryOfResidence(),
                req.idType(),
                req.idNumber(),
                req.riskRating(),
                req.comment(),
                req.province()
        );
    }
}
