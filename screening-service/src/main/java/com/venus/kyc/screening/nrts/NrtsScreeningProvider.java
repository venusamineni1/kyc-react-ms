package com.venus.kyc.screening.nrts;

import com.venus.kyc.screening.ScreeningDTOs;
import com.venus.kyc.screening.ScreeningProvider;
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
 */
@Component
@ConditionalOnProperty(name = "nrts.mock", havingValue = "false")
public class NrtsScreeningProvider implements ScreeningProvider {

    private static final Logger log = LoggerFactory.getLogger(NrtsScreeningProvider.class);

    private final NrtsConfig config;
    private final NrtsHttpClient httpClient;
    private final NrtsPayloadCodec codec;
    private final NrtsJsonParser jsonParser;

    public NrtsScreeningProvider(NrtsConfig config, NrtsHttpClient httpClient,
                                  NrtsPayloadCodec codec, NrtsJsonParser jsonParser) {
        this.config = config;
        this.httpClient = httpClient;
        this.codec = codec;
        this.jsonParser = jsonParser;
    }

    // ── Endpoint 1: Initiate ─────────────────────────────────────────────────

    @Override
    public ScreeningDTOs.InitiateScreeningResponse initiate(ScreeningDTOs.ScreeningInternalRequest request) {
        // 1. Build format-agnostic record and serialize
        NrtsRecord record = toNrtsRecord(request);
        String payload = codec.serializeSubmit(config.srcId(), List.of(record));
        log.debug("NRTS submit payload:\n{}", payload);

        // 2. Submit to NRTS
        NrtsHttpClient.NrtsRawResponse submitResponse = httpClient.submit(payload);
        log.info("NRTS submit HTTP {}", submitResponse.httpStatus());

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
        NrtsPayloadCodec.StatusResult statusResult =
                codec.parseStatusResponse(httpClient.getStatus(processId).body());

        NrtsPayloadCodec.ClientResult client = statusResult.clients().isEmpty()
                ? null : statusResult.clients().get(0);

        List<String> alertContexts = client == null ? Collections.emptyList()
                : client.alerts().stream()
                        .map(ScreeningDTOs.AlertContext::context)
                        .collect(Collectors.toList());

        Long reqId = client != null ? client.reqId() : null;

        log.info("NRTS initiate: Hot — processId={}, reqId={}, contexts={}", processId, reqId, alertContexts);
        return new ScreeningDTOs.InitiateScreeningResponse("Hot", processId, reqId, alertContexts);
    }

    // ── Endpoint 2: Status ───────────────────────────────────────────────────

    @Override
    public ScreeningDTOs.ScreeningStatusResponse checkStatus(long processId) {
        NrtsPayloadCodec.StatusResult result =
                codec.parseStatusResponse(httpClient.getStatus(processId).body());

        NrtsPayloadCodec.ClientResult client = result.clients().isEmpty()
                ? null : result.clients().get(0);

        List<ScreeningDTOs.ContextResult> contextResults = client == null
                ? Collections.emptyList()
                : client.alerts().stream()
                        .map(a -> new ScreeningDTOs.ContextResult(a.context(), a.status(), a.statusId()))
                        .collect(Collectors.toList());

        Long reqId = client != null ? client.reqId() : null;

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
        return jsonParser.parseDetailsResponse(reqId, raw.body());
    }

    // ── Endpoint 4: Document Download ────────────────────────────────────────

    @Override
    public ResponseEntity<byte[]> getDocument(String documentId) {
        return httpClient.getDocument(documentId);
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
