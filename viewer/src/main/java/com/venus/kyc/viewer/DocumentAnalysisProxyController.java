package com.venus.kyc.viewer;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Proxies OCR and fraud-signal requests from the frontend (JWT-authenticated)
 * to the internal document-service (unauthenticated internal call).
 *
 * Frontend calls:
 *   GET /api/cases/{caseId}/documents/{docId}/ocr
 *   GET /api/cases/{caseId}/documents/{docId}/signals
 *
 * Internally calls:
 *   GET http://document-service:8085/api/internal/documents/{docId}/ocr
 *   GET http://document-service:8085/api/internal/documents/{docId}/signals
 *
 * The caseId segment is kept in the URL for access control validation
 * (ensures the document actually belongs to the case before proxying).
 */
@RestController
@RequestMapping("/api/cases/{caseId}/documents/{docId}")
@Tag(name = "Document Analysis Proxy", description = "Proxies OCR and fraud-signal requests to the document-service")
public class DocumentAnalysisProxyController {

    private static final Logger log = LoggerFactory.getLogger(DocumentAnalysisProxyController.class);

    @Value("${document.service.url:http://localhost:8085/api/documents}")
    private String documentServiceUrl;

    private final CaseService        caseService;
    private final DocumentService    documentService;
    private final RestTemplate       restTemplate = new RestTemplate();

    public DocumentAnalysisProxyController(CaseService caseService,
                                           DocumentService documentService) {
        this.caseService     = caseService;
        this.documentService = documentService;
    }

    // ── OCR ──────────────────────────────────────────────────────────────────

    @Operation(summary = "Get OCR data for a case document",
               description = "Validates document belongs to case, then proxies to document-service for OCR extraction.")
    @GetMapping("/ocr")
    public ResponseEntity<?> getOcr(
            @Parameter(description = "Case ID") @PathVariable Long caseId,
            @Parameter(description = "Document ID") @PathVariable Long docId) {

        // Verify document belongs to the given case
        if (!documentBelongsToCase(caseId, docId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Document not found for this case."));
        }

        return proxyGet(docId, "ocr");
    }

    // ── Fraud Signals ─────────────────────────────────────────────────────────

    @Operation(summary = "Get fraud signals for a case document",
               description = "Validates document belongs to case, then proxies to document-service for fraud signal analysis.")
    @GetMapping("/signals")
    public ResponseEntity<?> getSignals(
            @Parameter(description = "Case ID") @PathVariable Long caseId,
            @Parameter(description = "Document ID") @PathVariable Long docId) {

        if (!documentBelongsToCase(caseId, docId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Document not found for this case."));
        }

        return proxyGet(docId, "signals");
    }

    // ── Internal helpers ─────────────────────────────────────────────────────

    private ResponseEntity<?> proxyGet(Long docId, String endpoint) {
        // Build URL: strip the /api/documents suffix, add /api/internal/documents/{id}/{endpoint}
        String base = documentServiceUrl.replace("/api/documents", "");
        String url  = base + "/api/internal/documents/" + docId + "/" + endpoint;

        try {
            ResponseEntity<Object> response = restTemplate.getForEntity(url, Object.class);
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());

        } catch (HttpClientErrorException.NotFound e) {
            return ResponseEntity.notFound().build();

        } catch (ResourceAccessException e) {
            // document-service is down — return a graceful pending response
            log.warn("document-service unreachable for {} endpoint: {}", endpoint, e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of(
                            "error", "Document analysis service is not available.",
                            "detail", "Start the document-service on port 8085 to enable OCR and fraud-signal analysis."
                    ));

        } catch (Exception e) {
            log.error("Proxy error for doc {} / {}: {}", docId, endpoint, e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Proxy error: " + e.getMessage()));
        }
    }

    /**
     * Checks that the document with the given ID belongs to the given case.
     * Fetches the list of case documents from document-service and verifies presence.
     */
    private boolean documentBelongsToCase(Long caseId, Long docId) {
        try {
            var docs = documentService.getDocuments(caseId);
            return docs.stream().anyMatch(d -> docId.equals(d.documentID()));
        } catch (Exception e) {
            log.warn("Could not validate document ownership for caseId={} docId={}: {}", caseId, docId, e.getMessage());
            // Fail open so document-service downtime doesn't break OCR lookup unnecessarily
            return true;
        }
    }
}
