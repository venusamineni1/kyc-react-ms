package com.venus.kyc.document.controller;

import com.venus.kyc.document.DocumentRepository;
import com.venus.kyc.document.model.FraudSignals;
import com.venus.kyc.document.model.OcrResult;
import com.venus.kyc.document.service.FraudSignalService;
import com.venus.kyc.document.service.OcrService;
import com.venus.kyc.document.service.TikaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Internal REST endpoints for document OCR and fraud-signal analysis.
 * Called by viewer-core — not directly exposed to the browser.
 *
 * Base path: /api/internal/documents
 */
@RestController
@RequestMapping("/api/internal/documents")
@Tag(name = "Document Analysis", description = "OCR and fraud signal analysis for uploaded documents")
public class DocumentAnalysisController {

    private static final Logger log = LoggerFactory.getLogger(DocumentAnalysisController.class);

    private final DocumentRepository  repository;
    private final OcrService          ocrService;
    private final FraudSignalService  fraudSignalService;
    private final TikaService         tikaService;

    public DocumentAnalysisController(DocumentRepository repository,
                                      OcrService ocrService,
                                      FraudSignalService fraudSignalService,
                                      TikaService tikaService) {
        this.repository        = repository;
        this.ocrService        = ocrService;
        this.fraudSignalService = fraudSignalService;
        this.tikaService       = tikaService;
    }

    // ── OCR ──────────────────────────────────────────────────────────────────

    /**
     * Run OCR analysis on a stored document.
     * GET /api/internal/documents/{id}/ocr
     */
    @Operation(summary = "Get OCR data for a stored document",
               description = "Runs the full OCR pipeline (MRZ + barcode + Tesseract) on the stored document bytes and returns extracted fields.")
    @GetMapping("/{id}/ocr")
    public ResponseEntity<OcrResult> getOcr(
            @Parameter(description = "Document ID") @PathVariable Long id) {

        return repository.findById(id)
                .map(doc -> {
                    String mime = resolveMime(doc.data(), doc.mimeType());
                    OcrResult result = ocrService.analyseDocument(doc.data(), mime);
                    return ResponseEntity.ok(result);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ── Fraud Signals ─────────────────────────────────────────────────────────

    /**
     * Return aggregated fraud signals for a stored document.
     * GET /api/internal/documents/{id}/signals
     */
    @Operation(summary = "Get fraud signals for a stored document",
               description = "Analyses MRZ check digits, PDF signatures, EXIF metadata, and barcode/OCR consistency.")
    @GetMapping("/{id}/signals")
    public ResponseEntity<FraudSignals> getSignals(
            @Parameter(description = "Document ID") @PathVariable Long id) {

        return repository.findById(id)
                .map(doc -> {
                    String mime = resolveMime(doc.data(), doc.mimeType());
                    OcrResult ocr = ocrService.analyseDocument(doc.data(), mime);
                    FraudSignals signals = fraudSignalService.analyse(doc.data(), mime, ocr);
                    return ResponseEntity.ok(signals);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ── OCR Preview (upload-time, no persistence) ─────────────────────────────

    /**
     * Run OCR on a freshly uploaded file without storing it.
     * POST /api/internal/documents/ocr-preview
     *
     * Useful for showing OCR results in an upload modal before the user confirms.
     */
    @Operation(summary = "OCR preview for an uploaded file",
               description = "Accepts a multipart file, runs the OCR pipeline, and returns extracted data — document is NOT stored.")
    @PostMapping(value = "/ocr-preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<OcrResult> ocrPreview(
            @RequestParam("file") MultipartFile file) {
        try {
            byte[] data = file.getBytes();
            String mime = resolveMime(data, file.getContentType());
            OcrResult result = ocrService.analyseDocument(data, mime);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("OCR preview failed", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Run all fraud signals on a freshly uploaded file (no persistence).
     * POST /api/internal/documents/signals-preview
     */
    @Operation(summary = "Fraud signals preview for an uploaded file",
               description = "Accepts a multipart file, runs all fraud detection signals, and returns results — document is NOT stored.")
    @PostMapping(value = "/signals-preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FraudSignals> signalsPreview(
            @RequestParam("file") MultipartFile file) {
        try {
            byte[] data = file.getBytes();
            String mime = resolveMime(data, file.getContentType());
            OcrResult ocr = ocrService.analyseDocument(data, mime);
            FraudSignals signals = fraudSignalService.analyse(data, mime, ocr);
            return ResponseEntity.ok(signals);
        } catch (Exception e) {
            log.error("Signals preview failed", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    /** Use Tika to detect MIME from raw bytes; fall back to stored mimeType. */
    private String resolveMime(byte[] data, String storedMime) {
        if (data == null) return storedMime;
        String detected = tikaService.detectMimeType(data);
        return (detected != null && !detected.equals("application/octet-stream"))
                ? detected : storedMime;
    }
}
