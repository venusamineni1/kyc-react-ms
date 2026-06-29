package com.venus.kyc.document.service;

import com.venus.kyc.document.model.Signal;
import com.venus.kyc.document.model.SignalLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Base64;
import java.util.Iterator;

/**
 * Error Level Analysis (ELA) — a classic forensic indicator for digitally edited image regions.
 *
 * How it works: a JPEG re-saved at a known quality compresses already-stable (untouched) areas
 * almost identically to the original, while any region edited after the document's last save
 * compresses differently because it hasn't been through the same number of compression cycles.
 * Diffing the original against a fresh re-save and amplifying the result makes those regions
 * visibly "hotter" than the rest of the image.
 *
 * This is a heuristic indicator, not proof of tampering — the resulting Signal is phrased
 * accordingly and the heatmap is meant to prompt human visual review, not auto-reject documents.
 *
 * Pure javax.imageio/java.awt.image — no OpenCV or other CV library required.
 */
@Service
public class ElaService {

    private static final Logger log = LoggerFactory.getLogger(ElaService.class);

    private static final float JPEG_RESAVE_QUALITY = 0.85f;
    private static final int AMPLIFICATION = 12;
    private static final int BLOCK_SIZE = 16;
    /** Minimum absolute gap between the hottest block and the median block to flag WARN. */
    private static final double HOTSPOT_DELTA_THRESHOLD = 35.0;

    private final PdfBoxService pdfBoxService;

    public ElaService(PdfBoxService pdfBoxService) {
        this.pdfBoxService = pdfBoxService;
    }

    public record ElaResult(Signal signal, String heatmapPngBase64) {}

    public ElaResult analyse(byte[] data, String mimeType) {
        try {
            BufferedImage original = decode(data, mimeType);
            if (original == null) {
                return new ElaResult(
                        new Signal(SignalLevel.PENDING, "ELA analysis unavailable for this document type."),
                        null);
            }

            BufferedImage resaved = jpegResave(original, JPEG_RESAVE_QUALITY);
            int width = Math.min(original.getWidth(), resaved.getWidth());
            int height = Math.min(original.getHeight(), resaved.getHeight());

            int[][] intensity = new int[height][width];
            BufferedImage heatmap = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int diff = maxChannelDiff(original.getRGB(x, y), resaved.getRGB(x, y));
                    int amplified = Math.min(255, diff * AMPLIFICATION);
                    intensity[y][x] = amplified;
                    heatmap.setRGB(x, y, rampColor(amplified));
                }
            }

            double[] blockMeans = computeBlockMeans(intensity, width, height);
            Signal signal = scoreSignal(blockMeans);

            return new ElaResult(signal, encodePngBase64(heatmap));
        } catch (Exception e) {
            log.warn("ELA analysis failed: {}", e.getMessage());
            return new ElaResult(
                    new Signal(SignalLevel.PENDING, "ELA analysis failed: " + e.getMessage()),
                    null);
        }
    }

    // ── Decode (mirrors OcrService's PDF/image dispatch) ────────────────────────

    private BufferedImage decode(byte[] data, String mimeType) throws Exception {
        if (data == null) return null;
        boolean isPdf = pdfBoxService.isPdf(data) || "application/pdf".equalsIgnoreCase(mimeType);
        if (isPdf) {
            byte[] pageBytes = pdfBoxService.renderFirstPageAsBytes(data);
            return pageBytes != null ? ImageIO.read(new ByteArrayInputStream(pageBytes)) : null;
        }
        if (mimeType != null && mimeType.startsWith("image/")) {
            return ImageIO.read(new ByteArrayInputStream(data));
        }
        return null;
    }

    // ── ELA core ──────────────────────────────────────────────────────────────

    private BufferedImage jpegResave(BufferedImage image, float quality) throws Exception {
        // Drop alpha — JPEG has no transparency, and ImageIO's JPEG writer rejects ARGB.
        BufferedImage rgb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        rgb.getGraphics().drawImage(image, 0, 0, null);

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        ImageWriter writer = writers.next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (MemoryCacheImageOutputStream out = new MemoryCacheImageOutputStream(baos)) {
            writer.setOutput(out);
            writer.write(null, new IIOImage(rgb, null, null), param);
        } finally {
            writer.dispose();
        }
        return ImageIO.read(new ByteArrayInputStream(baos.toByteArray()));
    }

    private int maxChannelDiff(int rgb1, int rgb2) {
        int dr = Math.abs(((rgb1 >> 16) & 0xFF) - ((rgb2 >> 16) & 0xFF));
        int dg = Math.abs(((rgb1 >> 8) & 0xFF) - ((rgb2 >> 8) & 0xFF));
        int db = Math.abs((rgb1 & 0xFF) - (rgb2 & 0xFF));
        return Math.max(dr, Math.max(dg, db));
    }

    /** Black -> dark gray -> yellow -> red, for a punchy thermal-style heatmap. */
    private int rampColor(int intensity) {
        float t = intensity / 255f;
        int r, g, b;
        if (t < 0.33f) {
            float local = t / 0.33f;
            r = g = b = (int) (60 * local);
        } else if (t < 0.66f) {
            float local = (t - 0.33f) / 0.33f;
            r = (int) (60 + (255 - 60) * local);
            g = (int) (60 + (255 - 60) * local);
            b = (int) (60 * (1 - local));
        } else {
            float local = (t - 0.66f) / 0.34f;
            r = 255;
            g = (int) (255 * (1 - local));
            b = 0;
        }
        return (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
    }

    private int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    private double[] computeBlockMeans(int[][] intensity, int width, int height) {
        java.util.List<Double> means = new java.util.ArrayList<>();
        for (int by = 0; by < height; by += BLOCK_SIZE) {
            for (int bx = 0; bx < width; bx += BLOCK_SIZE) {
                long sum = 0;
                int count = 0;
                for (int y = by; y < Math.min(by + BLOCK_SIZE, height); y++) {
                    for (int x = bx; x < Math.min(bx + BLOCK_SIZE, width); x++) {
                        sum += intensity[y][x];
                        count++;
                    }
                }
                if (count > 0) means.add(sum / (double) count);
            }
        }
        return means.stream().mapToDouble(Double::doubleValue).toArray();
    }

    private Signal scoreSignal(double[] blockMeans) {
        if (blockMeans.length == 0) {
            return new Signal(SignalLevel.PENDING, "Image too small for ELA block analysis.");
        }
        double[] sorted = blockMeans.clone();
        Arrays.sort(sorted);
        double median = sorted[sorted.length / 2];
        double max = sorted[sorted.length - 1];

        if (max - median > HOTSPOT_DELTA_THRESHOLD) {
            return new Signal(SignalLevel.WARN,
                    "Elevated compression artifacts detected in an isolated region — "
                            + "recommend visual inspection of the photo/data zone.");
        }
        return new Signal(SignalLevel.PASS,
                "No localized compression anomalies detected — error levels are consistent across the image.");
    }

    private String encodePngBase64(BufferedImage heatmap) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(heatmap, "PNG", baos);
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }
}
