package com.venus.kyc.screening;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock implementation of ScreeningProvider that simulates NRTS behaviour.
 * Active when nrts.mock=true (default for local dev).
 *
 * Deterministic hit logic:
 *  - Names containing "osama", "pablo", or "putin" → always Hot (PEP + SAN)
 *  - All others → 10% random Hot chance
 */
@Component
@ConditionalOnProperty(name = "nrts.mock", havingValue = "true", matchIfMissing = true)
public class MockScreeningProvider implements ScreeningProvider {

    private final Random random = new Random();

    /** processId → should-hit */
    private final Map<Long, Boolean> processHitMap = new ConcurrentHashMap<>();
    /** processId → reqId (single client per process) */
    private final Map<Long, Long> processReqIdMap = new ConcurrentHashMap<>();

    private static final String[] HIT_CONTEXTS = { "PEP", "SAN" };
    private static final String[] ALL_CONTEXTS  = { "PEP", "ADM", "INT", "SAN" };

    // ── Endpoint 1: Initiate ─────────────────────────────────────────────────

    @Override
    public ScreeningDTOs.InitiateScreeningResponse initiate(ScreeningDTOs.ScreeningInternalRequest request) {
        String name = (request.firstName() + " " + request.lastName()).toLowerCase();
        // Deterministic: Use hash of name to decide hit (consistent results for same client)
        boolean shouldHit = name.contains("osama") || name.contains("pablo") || name.contains("putin")
                || (Math.abs(name.hashCode()) % 100) < 10;

        if (!shouldHit) {
            return new ScreeningDTOs.InitiateScreeningResponse(
                    "No-Hit", null, null, Collections.emptyList());
        }

        // Simulate NRTS processId and reqId
        long processId = Math.abs(random.nextLong() % 9_000_000L) + 100_000L;
        long reqId     = Math.abs(random.nextLong() % 9_000_000L) + 100_000L;

        processHitMap.put(processId, true);
        processReqIdMap.put(processId, reqId);

        return new ScreeningDTOs.InitiateScreeningResponse(
                "Hot", processId, reqId, List.of("PEP", "SAN"));
    }

    // ── Endpoint 2: Status ───────────────────────────────────────────────────

    @Override
    public ScreeningDTOs.ScreeningStatusResponse checkStatus(long processId) {
        boolean isHit = processHitMap.getOrDefault(processId, false);
        Long reqId = processReqIdMap.get(processId);

        List<ScreeningDTOs.ContextResult> results = new ArrayList<>();
        if (isHit) {
            results.add(new ScreeningDTOs.ContextResult("PEP", "HIT", "PEP Match found in Mock DB"));
            results.add(new ScreeningDTOs.ContextResult("SAN", "HIT", "SAN Match found in Mock DB"));
            results.add(new ScreeningDTOs.ContextResult("ADM", "NO_HIT", null));
            results.add(new ScreeningDTOs.ContextResult("INT", "NO_HIT", null));
        } else {
            for (String ctx : ALL_CONTEXTS) {
                results.add(new ScreeningDTOs.ContextResult(ctx, "NO_HIT", null));
            }
        }

        // Mock always returns finalized immediately
        return new ScreeningDTOs.ScreeningStatusResponse(
                String.valueOf(processId),
                "Finished",
                true,
                reqId,
                results
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
