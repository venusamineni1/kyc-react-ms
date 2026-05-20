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
 */
@Component
@ConditionalOnProperty(name = "nrts.mock", havingValue = "false")
public class NrtsScreeningProvider implements ScreeningProvider {

    private static final Logger log = LoggerFactory.getLogger(NrtsScreeningProvider.class);

    private final NrtsConfig config;
    private final NrtsHttpClient httpClient;
    private final NrtsXmlBuilder xmlBuilder;
    private final NrtsXmlParser xmlParser;
    private final NrtsJsonParser jsonParser;

    public NrtsScreeningProvider(NrtsConfig config, NrtsHttpClient httpClient,
                                  NrtsXmlBuilder xmlBuilder, NrtsXmlParser xmlParser,
                                  NrtsJsonParser jsonParser) {
        this.config = config;
        this.httpClient = httpClient;
        this.xmlBuilder = xmlBuilder;
        this.xmlParser = xmlParser;
        this.jsonParser = jsonParser;
    }

    // ── Endpoint 1: Initiate ─────────────────────────────────────────────────

    @Override
    public ScreeningDTOs.InitiateScreeningResponse initiate(ScreeningDTOs.ScreeningInternalRequest request) {
        // 1. Build XML
        String xml = xmlBuilder.buildSubmitXml(config.srcId(), request);
        log.debug("NRTS submit XML:\n{}", xml);

        // 2. Submit to NRTS
        NrtsHttpClient.NrtsRawResponse submitResponse = httpClient.submit(xml);
        log.info("NRTS submit HTTP {}", submitResponse.httpStatus());

        NrtsXmlParser.NrtsSubmitResult submitResult = xmlParser.parseSubmitResponse(submitResponse.body());

        // 3. No alerts → return No-Hit immediately
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
        NrtsXmlParser.NrtsStatusResult statusResult = xmlParser.parseStatusResponse(
                httpClient.getStatus(processId).body()
        );

        // 5. Extract first (only) client result — always 1 client per submit
        NrtsXmlParser.NrtsClientResult client = statusResult.clients().isEmpty()
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
        NrtsHttpClient.NrtsRawResponse raw = httpClient.getStatus(processId);
        NrtsXmlParser.NrtsStatusResult result = xmlParser.parseStatusResponse(raw.body());

        NrtsXmlParser.NrtsClientResult client = result.clients().isEmpty()
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
}
