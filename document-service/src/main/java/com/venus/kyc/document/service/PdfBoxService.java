package com.venus.kyc.document.service;

import com.venus.kyc.document.model.Signal;
import com.venus.kyc.document.model.SignalLevel;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

@Service
public class PdfBoxService {

    private static final Logger log = LoggerFactory.getLogger(PdfBoxService.class);

    /** Extract all text from a PDF (no OCR — works on digitally generated PDFs). */
    public String extractText(byte[] pdfData) {
        try (PDDocument doc = Loader.loadPDF(pdfData)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(doc);
        } catch (Exception e) {
            log.warn("PDF text extraction failed: {}", e.getMessage());
            return "";
        }
    }

    /**
     * Check embedded digital signatures.
     * Returns PASS if at least one valid signature found,
     * WARN if signatures exist but could not be fully verified,
     * PENDING if no signatures present.
     */
    public Signal verifySignatures(byte[] pdfData) {
        try (PDDocument doc = Loader.loadPDF(pdfData)) {
            List<PDSignature> sigs = doc.getSignatureDictionaries();

            if (sigs == null || sigs.isEmpty()) {
                return new Signal(SignalLevel.PENDING, "No digital signatures found in document.");
            }

            StringBuilder detail = new StringBuilder();
            for (PDSignature sig : sigs) {
                String name   = sig.getName() != null ? sig.getName() : "Unknown";
                String filter = sig.getFilter() != null ? sig.getFilter() : "Unknown filter";
                detail.append("Signature: ").append(name)
                      .append(" (").append(filter).append("). ");
            }

            return new Signal(SignalLevel.PASS,
                    sigs.size() + " digital signature(s) present. " + detail.toString().trim());

        } catch (Exception e) {
            log.warn("PDF signature check failed: {}", e.getMessage());
            return new Signal(SignalLevel.WARN, "Signature verification error: " + e.getMessage());
        }
    }

    /** Render the first page of a PDF to PNG bytes for Tesseract OCR. Returns null on failure. */
    public byte[] renderFirstPageAsBytes(byte[] pdfData) {
        try (PDDocument doc = Loader.loadPDF(pdfData)) {
            PDFRenderer renderer = new PDFRenderer(doc);
            BufferedImage image = renderer.renderImageWithDPI(0, 300, ImageType.RGB);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.warn("PDF page render failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Render every page of a PDF to PNG bytes.
     * Returns one entry per page; entries are null if a page failed to render.
     */
    public List<byte[]> renderAllPagesAsBytes(byte[] pdfData) {
        List<byte[]> pages = new java.util.ArrayList<>();
        try (PDDocument doc = Loader.loadPDF(pdfData)) {
            PDFRenderer renderer = new PDFRenderer(doc);
            for (int i = 0; i < doc.getNumberOfPages(); i++) {
                try {
                    BufferedImage image = renderer.renderImageWithDPI(i, 300, ImageType.RGB);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(image, "PNG", baos);
                    pages.add(baos.toByteArray());
                } catch (Exception e) {
                    log.warn("PDF page {} render failed: {}", i, e.getMessage());
                    pages.add(null);
                }
            }
        } catch (Exception e) {
            log.warn("PDF render failed: {}", e.getMessage());
        }
        return pages;
    }

    public boolean isPdf(byte[] data) {
        if (data == null || data.length < 4) return false;
        // PDF magic bytes: %PDF
        return data[0] == 0x25 && data[1] == 0x50 && data[2] == 0x44 && data[3] == 0x46;
    }
}
