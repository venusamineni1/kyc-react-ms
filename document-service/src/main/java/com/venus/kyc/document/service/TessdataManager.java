package com.venus.kyc.document.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Extracts the bundled Tesseract language models from the classpath (resources/tessdata/)
 * to a temporary directory at first use so the JavaCPP Tesseract API can locate them.
 *
 * Works without any OS-level Tesseract installation because the native binaries
 * come from org.bytedeco:tesseract-platform, and the tessdata file is bundled
 * in src/main/resources/tessdata/eng.traineddata.
 */
@Component
public class TessdataManager {

    private static final Logger log = LoggerFactory.getLogger(TessdataManager.class);

    private static final String[] LANGUAGES = {"eng"};

    // Lazily extracted temp directory — shared for the lifetime of the JVM
    private final AtomicReference<Path> tessdataDirRef = new AtomicReference<>();

    /**
     * Returns a filesystem path to a directory containing the tessdata files.
     * The first call extracts them from the classpath; subsequent calls reuse the path.
     *
     * @throws IOException if extraction fails
     */
    public String getTessdataPath() throws IOException {
        Path dir = tessdataDirRef.get();
        if (dir != null) return dir.toString();

        // Create a stable temp dir (reused across restarts within the same JVM invocation)
        Path tempDir = Files.createTempDirectory("kyc-tessdata-");
        Path tessdataSubdir = tempDir.resolve("tessdata");
        Files.createDirectories(tessdataSubdir);

        for (String lang : LANGUAGES) {
            extractLanguageFile(lang, tessdataSubdir);
        }

        tessdataDirRef.set(tessdataSubdir);
        log.info("Tessdata extracted to: {}", tessdataSubdir);
        return tessdataSubdir.toString();
    }

    private void extractLanguageFile(String lang, Path targetDir) throws IOException {
        String resourcePath = "/tessdata/" + lang + ".traineddata";
        try (InputStream in = TessdataManager.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new FileNotFoundException(
                        "Tessdata resource not found: " + resourcePath +
                        ". Run './gradlew :document-service:downloadTessdata' to fetch it.");
            }
            Path out = targetDir.resolve(lang + ".traineddata");
            if (!Files.exists(out)) {
                Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING);
                log.debug("Extracted {} ({} bytes)", out.getFileName(), Files.size(out));
            }
        }
    }
}
