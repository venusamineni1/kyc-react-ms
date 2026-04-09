package com.venus.kyc.document.service;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.multi.GenericMultipleBarcodeReader;
import com.google.zxing.multi.MultipleBarcodeReader;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.javacpp.SizeTPointer;
import org.bytedeco.leptonica.PIX;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.*;

import static org.bytedeco.leptonica.global.leptonica.*;

/**
 * Decodes 1D/2D barcodes from images using ZXing.
 * Supports: PDF417 (driver licences), QR Code, Code 128, Code 39, EAN/UPC.
 */
@Service
public class BarcodeService {

    private static final Logger log = LoggerFactory.getLogger(BarcodeService.class);

    // Hint map: enable all useful formats and try harder
    private static final Map<DecodeHintType, Object> HINTS;
    static {
        Map<DecodeHintType, Object> h = new EnumMap<>(DecodeHintType.class);
        h.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        h.put(DecodeHintType.POSSIBLE_FORMATS, List.of(
                BarcodeFormat.PDF_417,
                BarcodeFormat.QR_CODE,
                BarcodeFormat.CODE_128,
                BarcodeFormat.CODE_39,
                BarcodeFormat.EAN_13,
                BarcodeFormat.DATA_MATRIX
        ));
        HINTS = Collections.unmodifiableMap(h);
    }

    /**
     * Attempt to decode all barcodes in an image.
     *
     * @param imageData raw image bytes (JPEG / PNG / BMP)
     * @return list of decoded barcode texts; empty if none found or image unreadable
     */
    public List<String> decodeAllBarcodes(byte[] imageData) {
        if (imageData == null || imageData.length == 0) return List.of();
        try {
            BufferedImage image = readImage(imageData);
            if (image == null) return List.of();

            // Try at original size first, then progressively downscaled.
            // ZXing struggles with very high-resolution images (e.g. PDFs rendered at 300 DPI
            // produce ~3000-4000px images). Scaling to ~1500px on the long side is the sweet spot.
            for (double scale : candidateScales(image)) {
                BufferedImage candidate = scale == 1.0 ? image : scaleImage(image, scale);
                List<String> found = tryDecodeImage(candidate);
                if (!found.isEmpty()) return found;
            }
            return List.of();

        } catch (Exception e) {
            log.warn("Barcode decode failed: {}", e.getMessage());
            return List.of();
        }
    }

    /** Returns the scale factors to try, starting at 1.0 and adding downscale steps for large images. */
    private double[] candidateScales(BufferedImage image) {
        int maxDim = Math.max(image.getWidth(), image.getHeight());
        if (maxDim <= 1500) return new double[]{1.0};
        if (maxDim <= 2500) return new double[]{1.0, 0.5};
        return new double[]{1.0, 0.5, 0.33, 0.25};
    }

    private List<String> tryDecodeImage(BufferedImage image) {
        try {
            LuminanceSource source = new BufferedImageLuminanceSource(image);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            MultipleBarcodeReader multiReader = new GenericMultipleBarcodeReader(new MultiFormatReader());
            Result[] results = multiReader.decodeMultiple(bitmap, HINTS);
            List<String> decoded = new ArrayList<>();
            for (Result r : results) {
                if (r.getText() != null && !r.getText().isBlank()) decoded.add(r.getText());
            }
            return decoded;
        } catch (NotFoundException e) {
            return List.of();
        }
    }

    private BufferedImage scaleImage(BufferedImage src, double factor) {
        int w = (int)(src.getWidth() * factor);
        int h = (int)(src.getHeight() * factor);
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return out;
    }

    /**
     * Attempt to decode the first barcode found (fast path).
     *
     * @param imageData raw image bytes
     * @return decoded text or null
     */
    public String decodeSingleBarcode(byte[] imageData) {
        if (imageData == null || imageData.length == 0) return null;
        try {
            BufferedImage image = readImage(imageData);
            if (image == null) return null;

            LuminanceSource source = new BufferedImageLuminanceSource(image);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

            Result result = new MultiFormatReader().decode(bitmap, HINTS);
            return result != null ? result.getText() : null;

        } catch (NotFoundException e) {
            return null;
        } catch (Exception e) {
            log.warn("Single barcode decode failed: {}", e.getMessage());
            return null;
        }
    }

    // ── Image reading ─────────────────────────────────────────────────────────

    /**
     * Read image bytes into a BufferedImage.
     * Tries Java's built-in ImageIO first (JPEG, PNG, BMP, GIF).
     * Falls back to Leptonica for formats Java doesn't support natively (WebP, TIFF, etc.).
     */
    private BufferedImage readImage(byte[] imageData) {
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageData));
            if (img != null) return img;
        } catch (Exception ignored) { }
        return readViaLeptonica(imageData);
    }

    /**
     * Use Leptonica (already on classpath via JavaCPP Tesseract) to decode the image,
     * write it as PNG in memory, then hand it back to ImageIO so ZXing can use it.
     * Handles WebP, TIFF, and any other format Leptonica supports.
     */
    private BufferedImage readViaLeptonica(byte[] imageData) {
        PIX pix = null;
        BytePointer imgPtr = null;
        PointerPointer<BytePointer> pdata = null;
        SizeTPointer psize = null;
        try {
            imgPtr = new BytePointer(imageData);
            pix = pixReadMem(imgPtr, imageData.length);
            if (pix == null || pix.isNull()) return null;

            pdata = new PointerPointer<>(1L);
            psize = new SizeTPointer(1);
            if (pixWriteMemPng(pdata, psize, pix, 0f) != 0) return null;

            BytePointer dataPtr = pdata.get(BytePointer.class);
            byte[] pngBytes = new byte[(int) psize.get()];
            dataPtr.get(pngBytes);
            dataPtr.deallocate();

            return ImageIO.read(new ByteArrayInputStream(pngBytes));
        } catch (Exception e) {
            log.warn("Leptonica image decode failed: {}", e.getMessage());
            return null;
        } finally {
            if (imgPtr != null) imgPtr.deallocate();
            if (pix    != null && !pix.isNull()) pixDestroy(pix);
            if (pdata  != null) pdata.deallocate();
            if (psize  != null) psize.deallocate();
        }
    }

    /**
     * Parse AAMVA-format PDF417 data from a North-American driver's licence.
     * Returns a map of field codes → values (e.g. "DCS" → surname, "DAC" → given name).
     * Returns empty map if the data doesn't look like AAMVA format.
     */
    public Map<String, String> parseAamva(String barcodeText) {
        Map<String, String> fields = new LinkedHashMap<>();
        if (barcodeText == null || !barcodeText.contains("ANSI ")) return fields;

        // Split on record separators / newlines
        String[] lines = barcodeText.split("[\\r\\n\u001e\u001c]+");
        for (String line : lines) {
            if (line.length() >= 3) {
                // Each field is a 3-char code followed by its value
                String code  = line.substring(0, 3);
                String value = line.substring(3).trim();
                if (code.matches("[A-Z]{3}") && !value.isEmpty()) {
                    fields.put(code, value);
                }
            }
        }
        return fields;
    }
}
