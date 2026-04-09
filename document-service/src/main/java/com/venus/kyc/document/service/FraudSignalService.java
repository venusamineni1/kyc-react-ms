package com.venus.kyc.document.service;

import com.venus.kyc.document.model.FraudSignals;
import com.venus.kyc.document.model.OcrResult;
import com.venus.kyc.document.model.Signal;
import com.venus.kyc.document.model.SignalLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Aggregates all fraud-detection signals for a document into a {@link FraudSignals} report.
 *
 * Signals:
 *   MRZ Check Digits — validated by MrzParserService (ICAO 9303)
 *   PDF Signature    — verified by PdfBoxService
 *   EXIF Metadata    — analysed by TikaService (editing-software, date mismatch)
 *   Photo Zone ELA   — Error Level Analysis placeholder (requires image-manipulation library)
 *   Face Detected    — placeholder (requires ML face-detection model)
 *   Barcode vs OCR   — cross-validates barcode-extracted name/DOB against OCR-extracted fields
 */
@Service
public class FraudSignalService {

    private static final Logger log = LoggerFactory.getLogger(FraudSignalService.class);

    private final TikaService    tikaService;
    private final PdfBoxService  pdfBoxService;
    private final OcrService     ocrService;
    private final BarcodeService barcodeService;
    private final MrzParserService mrzParser;

    public FraudSignalService(TikaService tikaService,
                               PdfBoxService pdfBoxService,
                               OcrService ocrService,
                               BarcodeService barcodeService,
                               MrzParserService mrzParser) {
        this.tikaService    = tikaService;
        this.pdfBoxService  = pdfBoxService;
        this.ocrService     = ocrService;
        this.barcodeService = barcodeService;
        this.mrzParser      = mrzParser;
    }

    /**
     * Run all available fraud checks on the raw document bytes and return the aggregated signals.
     *
     * @param data     raw document bytes
     * @param mimeType MIME type (e.g. "application/pdf", "image/jpeg")
     * @param ocr      pre-computed OCR result (pass null to skip OCR-dependent checks)
     */
    public FraudSignals analyse(byte[] data, String mimeType, OcrResult ocr) {
        FraudSignals signals = new FraudSignals();

        // 1. MRZ check digits
        signals.setMrzCheckDigits(mrzSignal(ocr));

        // 2. PDF digital signature
        signals.setPdfSignature(pdfSignatureSignal(data, mimeType));

        // 3. EXIF / metadata tampering
        signals.setExifMetadata(tikaService.analyseExifSignal(data));

        // 4. Photo-zone Error Level Analysis — not yet implemented (requires CV library)
        signals.setPhotoZoneEla(new Signal(SignalLevel.PENDING,
                "ELA analysis not available — requires computer-vision module."));

        // 5. Face detection — not yet implemented
        signals.setFaceDetected(new Signal(SignalLevel.PENDING,
                "Face detection not available — requires ML model."));

        // 6. Barcode vs OCR cross-validation
        signals.setBarcodeOcrMatch(barcodeOcrSignal(ocr));

        return signals;
    }

    // ── Individual signal builders ───────────────────────────────────────────

    private Signal mrzSignal(OcrResult ocr) {
        if (ocr == null || ocr.getMrzCheckDigits() == null) {
            return new Signal(SignalLevel.PENDING, "No MRZ zone detected in document.");
        }
        boolean pass = "PASS".equalsIgnoreCase(ocr.getMrzCheckDigits());
        return new Signal(
                pass ? SignalLevel.PASS : SignalLevel.FAIL,
                pass ? "All MRZ check digits are valid (ICAO 9303)."
                     : "One or more MRZ check digits failed validation.");
    }

    private Signal pdfSignatureSignal(byte[] data, String mimeType) {
        boolean isPdf = pdfBoxService.isPdf(data) || "application/pdf".equalsIgnoreCase(mimeType);
        if (!isPdf) {
            return new Signal(SignalLevel.PENDING, "Document is not a PDF — signature check skipped.");
        }
        return pdfBoxService.verifySignatures(data);
    }

    private Signal barcodeOcrSignal(OcrResult ocr) {
        if (ocr == null) {
            return new Signal(SignalLevel.PENDING, "OCR result not available for barcode cross-check.");
        }

        String barcodeData = ocr.getBarcodeData();
        if (barcodeData == null || barcodeData.isBlank()) {
            return new Signal(SignalLevel.PENDING, "No barcode found in document.");
        }

        // Try to cross-validate name from barcode vs MRZ / OCR
        java.util.Map<String, String> aamva = barcodeService.parseAamva(barcodeData);
        if (aamva.isEmpty()) {
            return new Signal(SignalLevel.PASS,
                    "Barcode found but not AAMVA format — cross-check skipped.");
        }

        StringBuilder detail = new StringBuilder();
        boolean mismatch = false;

        // Compare surname
        String barcodeSurname = aamva.get("DCS");
        if (barcodeSurname != null && ocr.getSurname() != null) {
            String ocrSurname = ocr.getSurname().value();
            if (!normalise(barcodeSurname).equals(normalise(ocrSurname))) {
                detail.append("Surname mismatch: barcode='").append(barcodeSurname)
                      .append("' vs OCR='").append(ocrSurname).append("'. ");
                mismatch = true;
            }
        }

        // Compare licence number
        String barcodeDocNum = aamva.get("DAQ");
        if (barcodeDocNum != null && ocr.getDocumentNumber() != null) {
            String ocrDocNum = ocr.getDocumentNumber().value();
            if (!normalise(barcodeDocNum).equals(normalise(ocrDocNum))) {
                detail.append("Document number mismatch: barcode='").append(barcodeDocNum)
                      .append("' vs OCR='").append(ocrDocNum).append("'. ");
                mismatch = true;
            }
        }

        if (detail.isEmpty()) {
            detail.append("Barcode and OCR data are consistent.");
        }

        return new Signal(mismatch ? SignalLevel.WARN : SignalLevel.PASS, detail.toString().trim());
    }

    private String normalise(String s) {
        return s == null ? "" : s.trim().toUpperCase().replaceAll("[^A-Z0-9]", "");
    }
}
