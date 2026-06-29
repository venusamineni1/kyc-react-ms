package com.venus.kyc.screening;

import com.venus.kyc.screening.nrts.NrtsInteractionRecorder;
import com.venus.kyc.screening.nrts.NrtsPayloadCodec;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Mock implementation of ScreeningProvider that simulates NRTS behaviour.
 * Active when nrts.mock=true (default for local dev).
 *
 * Deterministic hit logic:
 *  - Names containing "osama", "pablo", or "putin" → always Hot (PEP + SAN)
 *  - All others → 10% random Hot chance
 *
 * Unlike a naive mock, this generates real NRTS-shaped XML for both the submit response
 * and the status response, then parses that XML back through the same NrtsPayloadCodec
 * the real NrtsScreeningProvider uses — so the mock's output is validated by the real
 * parsing code, not just hand-built DTOs. Every call is also recorded as an interaction
 * row via NrtsInteractionRecorder, exactly like the real provider, so dev/test produces
 * the same shape of audit data as production.
 *
 * Finalization is also simulated realistically: a Hit's first status check (the immediate
 * one inside initiate(), mirroring real NRTS behaviour) comes back NOT finalized; only a
 * later, explicit checkStatus() poll returns finalized=true — matching how a real NRTS
 * investigation actually progresses.
 */
@Component
@ConditionalOnProperty(name = "nrts.mock", havingValue = "true", matchIfMissing = true)
public class MockScreeningProvider implements ScreeningProvider {

    private final Random random = new Random();
    private final NrtsPayloadCodec codec;
    private final NrtsInteractionRecorder interactionRecorder;
    private final ScreeningRepository screeningRepository;

    /** processId → should-hit */
    private final Map<Long, Boolean> processHitMap = new ConcurrentHashMap<>();
    /** processId → reqId (single client per process) */
    private final Map<Long, Long> processReqIdMap = new ConcurrentHashMap<>();
    /** processId → number of status checks so far (1st = not finalized, 2nd+ = finalized) */
    private final Map<Long, AtomicInteger> processPollCountMap = new ConcurrentHashMap<>();

    public MockScreeningProvider(NrtsPayloadCodec codec, NrtsInteractionRecorder interactionRecorder,
                                  ScreeningRepository screeningRepository) {
        this.codec = codec;
        this.interactionRecorder = interactionRecorder;
        this.screeningRepository = screeningRepository;
    }

    // ── Endpoint 1: Initiate ─────────────────────────────────────────────────

    @Override
    public ScreeningDTOs.InitiateScreeningResponse initiate(ScreeningDTOs.ScreeningInternalRequest request, Long screeningLogId) {
        String name = (request.firstName() + " " + request.lastName()).toLowerCase();
        // Deterministic: Use hash of name to decide hit (consistent results for same client)
        boolean shouldHit = name.contains("osama") || name.contains("pablo") || name.contains("putin")
                || (Math.abs(name.hashCode()) % 100) < 10;

        Long processId = shouldHit ? Math.abs(random.nextLong() % 9_000_000L) + 100_000L : null;

        String submitResponseXml = buildSubmitResponseXml(shouldHit, processId);
        NrtsPayloadCodec.SubmitResult submitResult = codec.parseSubmitResponse(submitResponseXml);

        interactionRecorder.record(screeningLogId, request.clientId(), null, null, null,
                "SUBMIT", 200, "MOCK POST /nrts/submit", submitResponseXml, !submitResult.anyAlerts());

        if (!submitResult.anyAlerts()) {
            return new ScreeningDTOs.InitiateScreeningResponse(
                    "No-Hit", null, null, Collections.emptyList());
        }

        long reqId = Math.abs(random.nextLong() % 9_000_000L) + 100_000L;
        processHitMap.put(processId, true);
        processReqIdMap.put(processId, reqId);

        // Mirrors the real provider: an immediate first get_status call right after submit.
        // Real NRTS does not finalize a Hit on the very first poll, so this comes back pending.
        int pollCount = processPollCountMap.computeIfAbsent(processId, k -> new AtomicInteger(0)).incrementAndGet();
        String statusXml = buildStatusResponseXml(processId, reqId, request.clientId(), true, pollCount >= 2);
        NrtsPayloadCodec.StatusResult statusResult = codec.parseStatusResponse(statusXml);

        interactionRecorder.record(screeningLogId, request.clientId(), null, processId, reqId,
                "STATUS_POLL", 200, "MOCK GET /nrts/get_status/" + processId, statusXml,
                statusResult.isFinalized());

        List<String> alertContexts = extractAlertContexts(statusResult);

        return new ScreeningDTOs.InitiateScreeningResponse("Hot", processId, reqId, alertContexts);
    }

    // ── Endpoint 2: Status ───────────────────────────────────────────────────

    @Override
    public ScreeningDTOs.ScreeningStatusResponse checkStatus(long processId) {
        boolean isHit = processHitMap.getOrDefault(processId, false);
        Long reqId = processReqIdMap.get(processId);

        int pollCount = processPollCountMap.computeIfAbsent(processId, k -> new AtomicInteger(1)).incrementAndGet();
        boolean finalized = !isHit || pollCount >= 2;

        ScreeningLog existingLog = screeningRepository.findLogByNrtsProcessId(processId);
        String statusXml = buildStatusResponseXml(processId, reqId, existingLog != null ? existingLog.clientID() : null,
                isHit, finalized);
        NrtsPayloadCodec.StatusResult result = codec.parseStatusResponse(statusXml);

        interactionRecorder.record(existingLog != null ? existingLog.logID() : null,
                existingLog != null ? existingLog.clientID() : null,
                null, processId, reqId, "STATUS_POLL", 200,
                "MOCK GET /nrts/get_status/" + processId, statusXml, result.isFinalized());

        NrtsPayloadCodec.ClientResult client = result.clients().isEmpty() ? null : result.clients().get(0);
        List<ScreeningDTOs.ContextResult> contextResults = client == null
                ? Collections.emptyList()
                : client.alerts().stream()
                        .map(a -> new ScreeningDTOs.ContextResult(a.context(), a.status(), buildAlertMessage(a)))
                        .collect(Collectors.toList());

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
        String mockDate = "2024-01-15T10:30:00.000Z";

        List<ScreeningDTOs.DecisionEntry> decisions = List.of(
                new ScreeningDTOs.DecisionEntry(
                        mockDate, "MOCK_OPERATOR", "270", "Confirmed match",
                        new ScreeningDTOs.DocumentRef(
                                "{MOCK-DOC-0001-0000-0000-000000000001}", "Supporting doc", null)),
                new ScreeningDTOs.DecisionEntry(
                        "2024-01-14T09:00:00.000Z", "FSK", "0",
                        "Suspect(s) detected by FircoSoft Filter", null)
        );

        List<ScreeningDTOs.AlertEntry> alerts = List.of(
                new ScreeningDTOs.AlertEntry(
                        "0001_PEP!MOCK_" + reqId, "PEP", "17",
                        mockDate, "MOCK_OPERATOR", "Confirmed PEP match",
                        List.of(new ScreeningDTOs.HitEntry(
                                "UNITED STATES", "WASHINGTON DC", "MOCK SANCTIONED PERSON",
                                "WORLDCHECK", "OFAC SDN PEP", "I")),
                        decisions,
                        List.of(new ScreeningDTOs.DocumentRef(
                                "{MOCK-DOC-0002-0000-0000-000000000002}", "Alert level doc", "MOCK_OPERATOR"))
                )
        );

        return new ScreeningDTOs.AlertDetailsResponse(reqId, "FINISHED", alerts);
    }

    // ── Endpoint 4: Document Download ────────────────────────────────────────

    @Override
    public ResponseEntity<byte[]> getDocument(String documentId) {
        // Return a minimal valid PDF as mock binary content
        byte[] mockPdf = buildMockPdf(documentId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentLength(mockPdf.length);
        headers.setContentDisposition(
                ContentDisposition.formData()
                        .name("attachment")
                        .filename("mock-document.pdf")
                        .build());

        return ResponseEntity.ok().headers(headers).body(mockPdf);
    }

    // ── Mock XML generation ──────────────────────────────────────────────────
    // Plain, unprefixed XML — NrtsXmlCodec's parser matches elements by localName via
    // getElementsByTagNameNS("*", localName), so no namespace declarations are required.

    private String buildSubmitResponseXml(boolean anyAlerts, Long processId) {
        StringBuilder xml = new StringBuilder("<Response>");
        xml.append("<Stat>OK</Stat>");
        xml.append("<AnyAlerts>").append(anyAlerts ? "T" : "F").append("</AnyAlerts>");
        if (processId != null) {
            xml.append("<ProcessId>").append(processId).append("</ProcessId>");
        }
        xml.append("<Msg>Mock submission accepted</Msg>");
        xml.append("</Response>");
        return xml.toString();
    }

    private String buildStatusResponseXml(Long processId, Long reqId, Long clientId,
                                           boolean isHit, boolean finalized) {
        StringBuilder xml = new StringBuilder("<Response>");
        xml.append("<Stat>").append(finalized ? "FINISHED" : "PENDING").append("</Stat>");
        xml.append("<ProcId>").append(processId).append("</ProcId>");
        xml.append("<NoR>1</NoR>");
        xml.append("<Result>");
        xml.append("<ReqId>").append(reqId).append("</ReqId>");
        xml.append("<ClientId>").append(clientId != null ? clientId : "").append("</ClientId>");
        xml.append("<Type>I</Type>");
        xml.append("<Name>MOCK</Name>");
        xml.append("<Final>").append(finalized ? "T" : "F").append("</Final>");
        if (isHit) {
            xml.append(buildAlertXml("PEP", "HIT", "17"));
            xml.append(buildAlertXml("SAN", "HIT", "17"));
            xml.append(buildAlertXml("ADM", "NO_HIT", null));
            xml.append(buildAlertXml("INT", "NO_HIT", null));
        } else {
            for (String ctx : new String[] { "PEP", "ADM", "INT", "SAN" }) {
                xml.append(buildAlertXml(ctx, "NO_HIT", null));
            }
        }
        xml.append("</Result>");
        xml.append("</Response>");
        return xml.toString();
    }

    private String buildAlertXml(String context, String status, String statusId) {
        return "<Alert><Context>" + context + "</Context><Status>" + status + "</Status>"
                + (statusId != null ? "<StatusId>" + statusId + "</StatusId>" : "")
                + "</Alert>";
    }

    private List<String> extractAlertContexts(NrtsPayloadCodec.StatusResult statusResult) {
        NrtsPayloadCodec.ClientResult client = statusResult.clients().isEmpty()
                ? null : statusResult.clients().get(0);
        if (client == null) return Collections.emptyList();
        return client.alerts().stream()
                .filter(a -> "HIT".equalsIgnoreCase(a.status()))
                .map(ScreeningDTOs.AlertContext::context)
                .collect(Collectors.toList());
    }

    /**
     * NRTS get_status only returns a numeric StatusId per alert, not free-text — synthesize a
     * readable message from it instead of surfacing the bare code to analysts (e.g. "17").
     */
    private String buildAlertMessage(ScreeningDTOs.AlertContext alert) {
        if (!"HIT".equalsIgnoreCase(alert.status()) || alert.statusId() == null) return null;
        return alert.context() + " match flagged by NRTS (status code " + alert.statusId() + ")";
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Returns a minimal 1-page PDF bytestream for testing. */
    private byte[] buildMockPdf(String documentId) {
        String content = "%PDF-1.4\n1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n" +
                "2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj\n" +
                "3 0 obj<</Type/Page/MediaBox[0 0 612 792]/Parent 2 0 R/Contents 4 0 R/Resources<<>>>>\nendobj\n" +
                "4 0 obj<</Length 44>>\nstream\nBT /F1 12 Tf 100 700 Td (Mock Doc: " + documentId + ") Tj ET\nendstream\nendobj\n" +
                "xref\n0 5\n0000000000 65535 f\ntrailer<</Size 5/Root 1 0 R>>\nstartxref\n9\n%%EOF";
        return content.getBytes();
    }
}
