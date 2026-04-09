package com.venus.kyc.document.service;

import com.venus.kyc.document.model.Signal;
import com.venus.kyc.document.model.SignalLevel;
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.metadata.XMPDM;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

@Service
public class TikaService {

    private static final Logger log = LoggerFactory.getLogger(TikaService.class);
    private final Tika tika = new Tika();

    /** Detect MIME type from raw bytes (ignores file extension). */
    public String detectMimeType(byte[] data) {
        try {
            return tika.detect(data);
        } catch (Exception e) {
            log.warn("MIME detection failed: {}", e.getMessage());
            return "application/octet-stream";
        }
    }

    /** Extract all metadata key-value pairs using Tika's AutoDetectParser. */
    public Map<String, String> extractMetadata(byte[] data) {
        Map<String, String> result = new HashMap<>();
        try {
            Metadata metadata = new Metadata();
            BodyContentHandler handler = new BodyContentHandler(-1);
            AutoDetectParser parser = new AutoDetectParser();
            parser.parse(new ByteArrayInputStream(data), handler, metadata);

            for (String name : metadata.names()) {
                String value = metadata.get(name);
                if (value != null && !value.isBlank()) {
                    result.put(name, value);
                }
            }
        } catch (Exception e) {
            log.warn("Metadata extraction failed: {}", e.getMessage());
        }
        return result;
    }

    /**
     * Analyse EXIF metadata for tampering signals:
     * - Software tag (Photoshop, GIMP, etc.) → WARN
     * - Modified date newer than creation date → WARN
     * - GPS coordinates present (for cross-check) → note in detail
     */
    public Signal analyseExifSignal(byte[] data) {
        try {
            Map<String, String> meta = extractMetadata(data);

            StringBuilder detail = new StringBuilder();
            boolean hasWarn = false;

            // Check editing software
            String software = meta.getOrDefault("Software", meta.getOrDefault("tiff:Software", ""));
            if (!software.isBlank()) {
                String lc = software.toLowerCase();
                if (lc.contains("photoshop") || lc.contains("gimp") || lc.contains("lightroom")
                        || lc.contains("affinity") || lc.contains("paint")) {
                    detail.append("Image edited with ").append(software).append(". ");
                    hasWarn = true;
                } else {
                    detail.append("Captured by: ").append(software).append(". ");
                }
            }

            // Check creation vs modification date
            String created  = meta.getOrDefault(TikaCoreProperties.CREATED.getName(),
                              meta.getOrDefault("meta:creation-date", ""));
            String modified = meta.getOrDefault(TikaCoreProperties.MODIFIED.getName(),
                              meta.getOrDefault("Last-Modified", ""));
            if (!created.isBlank() && !modified.isBlank() && !created.equals(modified)) {
                detail.append("Modified date differs from creation date. ");
                hasWarn = true;
            }

            // GPS coordinates
            String gpsLat = meta.getOrDefault("GPS Latitude", "");
            String gpsLon = meta.getOrDefault("GPS Longitude", "");
            if (!gpsLat.isBlank()) {
                detail.append("GPS: ").append(gpsLat).append(", ").append(gpsLon).append(". ");
            }

            if (detail.isEmpty()) detail.append("No suspicious metadata found.");

            return new Signal(hasWarn ? SignalLevel.WARN : SignalLevel.PASS, detail.toString().trim());

        } catch (Exception e) {
            log.warn("EXIF analysis failed: {}", e.getMessage());
            return new Signal(SignalLevel.PENDING, "EXIF extraction failed: " + e.getMessage());
        }
    }
}
