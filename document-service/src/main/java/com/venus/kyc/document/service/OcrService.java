package com.venus.kyc.document.service;

import com.venus.kyc.document.model.OcrField;
import com.venus.kyc.document.model.OcrResult;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.leptonica.PIX;
import org.bytedeco.tesseract.TessBaseAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import static org.bytedeco.leptonica.global.leptonica.pixDestroy;
import static org.bytedeco.leptonica.global.leptonica.pixReadMem;
import static org.bytedeco.leptonica.global.leptonica.pixScale;
import static org.bytedeco.tesseract.global.tesseract.OEM_LSTM_ONLY;

/**
 * OCR pipeline using JavaCPP Tesseract — no OS installation required.
 * Native Tesseract binaries come from org.bytedeco:tesseract-platform.
 * Language models (eng.traineddata) are bundled in src/main/resources/tessdata/
 * and extracted to a temp directory at first use by {@link TessdataManager}.
 *
 * Set ocr.mock=true to skip OCR and return synthetic data (useful in CI / offline).
 */
@Service
public class OcrService {

    private static final Logger log = LoggerFactory.getLogger(OcrService.class);

    @Value("${ocr.mock:true}")
    private boolean mockMode;

    @Value("${ocr.language:eng}")
    private String ocrLanguage;

    private final MrzParserService mrzParser;
    private final BarcodeService   barcodeService;
    private final PdfBoxService    pdfBoxService;
    private final TessdataManager  tessdataManager;

    public OcrService(MrzParserService mrzParser,
                      BarcodeService barcodeService,
                      PdfBoxService pdfBoxService,
                      TessdataManager tessdataManager) {
        this.mrzParser      = mrzParser;
        this.barcodeService = barcodeService;
        this.pdfBoxService  = pdfBoxService;
        this.tessdataManager = tessdataManager;
    }

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * Full OCR pipeline for a document.
     *  PDF  → extract embedded text via PDFBox; attempt MRZ parse on text
     *  Image → run Tesseract (or mock); attempt MRZ + barcode parse
     */
    public OcrResult analyseDocument(byte[] data, String mimeType) {
        if (data == null || mimeType == null) return emptyResult("Unknown");

        boolean isPdf = pdfBoxService.isPdf(data) || "application/pdf".equalsIgnoreCase(mimeType);

        if (isPdf) {
            return analysePdf(data);
        } else if (mimeType.startsWith("image/")) {
            return analyseImage(data);
        }

        return emptyResult(mimeType);
    }

    // ── PDF path ─────────────────────────────────────────────────────────────

    private OcrResult analysePdf(byte[] data) {
        String text = pdfBoxService.extractText(data);

        // Try MRZ extraction from embedded text first (fast, works on generated PDFs)
        if (text != null && !text.isBlank()) {
            OcrResult result = mrzParser.parseMrzFromText(text);
            if (result != null) {
                result.setRawText(text);
                result.setSource("PDF/Text");
                return result;
            }
            log.info("PDF has embedded text but no MRZ found; falling back to per-page image OCR");
        } else {
            log.info("PDF has no embedded text; falling back to per-page image OCR");
        }

        // Render every page and run image analysis on each.
        // Scan pages in order, stopping as soon as MRZ fields are found.
        // If no MRZ is found on any page, return the first page that had barcode data.
        List<byte[]> pages = pdfBoxService.renderAllPagesAsBytes(data);
        log.info("PDF has {} page(s) to scan for barcodes/MRZ", pages.size());

        OcrResult barcodeResult = null; // best barcode-only result (no MRZ fields)

        for (int i = 0; i < pages.size(); i++) {
            byte[] pageBytes = pages.get(i);
            if (pageBytes == null) continue;

            OcrResult pageResult = analyseImage(pageBytes);
            String pageLabel = mockMode ? "OCR/Mock" : "OCR/Scanned-PDF (page " + (i + 1) + ")";
            pageResult.setSource(pageLabel);

            if (hasMrzFields(pageResult)) {
                // MRZ found — stop immediately, no need to check further pages
                log.info("MRZ fields found on page {}", i + 1);
                return pageResult;
            }

            if (pageResult.getBarcodeData() != null && barcodeResult == null) {
                // Keep the first page that yielded barcode data as a fallback
                barcodeResult = pageResult;
            }
        }

        // No MRZ on any page — return barcode result if we got one, otherwise empty
        return barcodeResult != null ? barcodeResult : emptyResult("PDF");
    }

    /** True when the result has at least one parsed identity field from an MRZ or barcode. */
    private boolean hasMrzFields(OcrResult r) {
        return r.getSurname() != null || r.getGivenNames() != null || r.getDocumentNumber() != null;
    }

    // ── Image path ───────────────────────────────────────────────────────────

    private OcrResult analyseImage(byte[] data) {
        // 1. Barcode decode (fast — before slow OCR)
        List<String> barcodes = barcodeService.decodeAllBarcodes(data);
        String barcodeText = barcodes.isEmpty() ? null : String.join("\n", barcodes);

        // 2. OCR
        String rawText = mockMode ? mockOcrText() : runTesseract(data);

        // 3. MRZ parse from OCR text
        OcrResult result = null;
        if (rawText != null && !rawText.isBlank()) {
            result = mrzParser.parseMrzFromText(rawText);
        }
        if (result == null) result = emptyResult("Image");

        result.setRawText(rawText);
        result.setSource(mockMode ? "OCR/Mock" : "OCR/Tesseract");

        // 4. Enrich with barcode / AAMVA data
        if (barcodeText != null) {
            result.setBarcodeData(barcodeText);
            Map<String, String> aamva = barcodeService.parseAamva(barcodeText);
            if (!aamva.isEmpty()) {
                enrichFromAamva(result, aamva);
            }
        }

        return result;
    }

    // ── JavaCPP Tesseract ────────────────────────────────────────────────────

    /**
     * Run Tesseract on raw image bytes using JavaCPP (no native install needed).
     * Uses in-memory leptonica pixReadMem to avoid writing temp files.
     */
    private String runTesseract(byte[] data) {
        TessBaseAPI api = null;
        PIX pix = null;
        BytePointer textPtr = null;

        try {
            String tessdataPath = tessdataManager.getTessdataPath();

            api = new TessBaseAPI();
            // Init with OEM_LSTM_ONLY — tessdata_fast contains only LSTM models,
            // so OEM_DEFAULT would fail trying to load the (absent) legacy model.
            // dataPath must be the parent of the "tessdata" folder.
            if (api.Init(tessdataPath, ocrLanguage, OEM_LSTM_ONLY) != 0) {
                log.error("Tesseract Init failed — check tessdata path: {}", tessdataPath);
                return null;
            }

            // PSM 6 = assume uniform block of text (good for document pages)
            api.SetPageSegMode(6);

            // Load image directly from byte array via leptonica — no temp file needed
            BytePointer imgBytes = new BytePointer(data);
            pix = pixReadMem(imgBytes, data.length);
            imgBytes.deallocate();

            if (pix == null || pix.isNull()) {
                log.warn("Leptonica could not decode image ({} bytes)", data.length);
                return null;
            }

            // Upscale small images — Tesseract LSTM needs ~30px per character height.
            // Documents smaller than 1000px on their longest side (e.g. 250×175 thumbnail
            // passports) produce too few pixels per character for reliable OCR.
            int maxDim = Math.max(pix.w(), pix.h());
            if (maxDim < 1000) {
                float scale = 1000.0f / maxDim;
                log.info("Upscaling small image {}x{} by {}x for OCR", pix.w(), pix.h(), String.format("%.1f", scale));
                PIX scaled = pixScale(pix, scale, scale);
                pixDestroy(pix);
                pix = scaled;
            }

            api.SetImage(pix);
            textPtr = api.GetUTF8Text();

            return (textPtr != null && !textPtr.isNull()) ? textPtr.getString() : null;

        } catch (Exception e) {
            log.error("Tesseract OCR failed: {}", e.getMessage(), e);
            return null;
        } finally {
            if (textPtr != null && !textPtr.isNull()) textPtr.deallocate();
            if (pix     != null && !pix.isNull())     pixDestroy(pix);
            if (api     != null && !api.isNull())     api.End();
        }
    }

    // ── Mock data ────────────────────────────────────────────────────────────

    /** Synthetic passport MRZ for demo/CI — no Tesseract needed. */
    private String mockOcrText() {
        return """
                REPUBLIC OF EXAMPLE
                PASSPORT
                PDEXAJOHNSON<<MICHAEL<<<<<<<<<<<<<<<<<<<<
                EX1234567<2EXA9001015M2801015<<<<<<<<<<<<4
                """;
    }

    // ── AAMVA enrichment ─────────────────────────────────────────────────────

    private void enrichFromAamva(OcrResult result, Map<String, String> aamva) {
        if (result.getSurname() == null) {
            String dcs = aamva.get("DCS");
            if (dcs != null) result.setSurname(new OcrField(dcs, 0.95, "Barcode/AAMVA"));
        }
        if (result.getGivenNames() == null) {
            String dac = aamva.getOrDefault("DAC", "");
            String dad = aamva.getOrDefault("DAD", "");
            String given = (dac + (dad.isBlank() ? "" : " " + dad)).trim();
            if (!given.isBlank()) result.setGivenNames(new OcrField(given, 0.95, "Barcode/AAMVA"));
        }
        if (result.getDateOfBirth() == null) {
            String dbb = aamva.get("DBB"); // MMDDYYYY
            if (dbb != null && dbb.length() == 8) {
                String fmt = dbb.substring(6) + "/" + dbb.substring(0, 2) + "/" + dbb.substring(2, 4);
                result.setDateOfBirth(new OcrField(fmt, 0.95, "Barcode/AAMVA"));
            }
        }
        if (result.getDocumentNumber() == null) {
            String daq = aamva.get("DAQ");
            if (daq != null) result.setDocumentNumber(new OcrField(daq, 0.98, "Barcode/AAMVA"));
        }
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private OcrResult emptyResult(String source) {
        OcrResult r = new OcrResult();
        r.setSource(source);
        return r;
    }
}
