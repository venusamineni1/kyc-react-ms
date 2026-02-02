package com.venus.kyc.screening.batch;

import com.venus.kyc.screening.batch.model.NLSFeed;
import com.venus.kyc.screening.batch.model.Feedback;
import com.venus.kyc.screening.batch.model.Notification;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;

@Service
public class ResponsePoller {

    private final SftpService sftpService;
    private final EncryptionService encryptionService;
    private final CompressionService compressionService;

    @Value("${batch.work.dir:/tmp/screening-batch}")
    private String workDir;

    @Value("${batch.sftp.download.dir:download}")
    private String downloadDir;

    @Value("${batch.privateKeyPath}")
    private String privateKeyPath;

    @Value("${batch.passphrase:}")
    private String passphrase;

    public ResponsePoller(SftpService sftpService, EncryptionService encryptionService,
            CompressionService compressionService) {
        this.sftpService = sftpService;
        this.encryptionService = encryptionService;
        this.compressionService = compressionService;
    }

    @Scheduled(fixedDelay = 60000) // Poll every minute
    public void pollForResponses() {
        try {
            List<String> files = sftpService.listFiles(downloadDir);
            for (String filename : files) {
                if (filename.endsWith(".zip.gpg")) {
                    processFile(filename);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processFile(String filename) throws Exception {
        System.out.println("Processing response file: " + filename);
        File localWorkDir = new File(workDir, "response_" + System.currentTimeMillis());
        localWorkDir.mkdirs();

        // 1. Download
        File encryptedFile = new File(localWorkDir, filename);
        sftpService.downloadFile(downloadDir + "/" + filename, encryptedFile);

        // 2. Decrypt
        File zipFile = new File(localWorkDir, filename.replace(".gpg", ""));
        try (FileInputStream keyIS = new FileInputStream(privateKeyPath)) {
            encryptionService.decryptFile(encryptedFile, zipFile, keyIS, passphrase);
        }

        // 3. Unzip
        File extractedDir = new File(localWorkDir, "extracted");
        List<File> extractedFiles = compressionService.unzipFile(zipFile, extractedDir);

        // 4. Parse XML
        for (File file : extractedFiles) {
            if (file.getName().endsWith(".xml")) {
                parseAndProcess(file);
            } else if (file.getName().endsWith(".sha256sum")) {
                // Verify checksum logic here
            }
        }

        // Cleanup? sftpService.deleteFile(downloadDir + "/" + filename);
    }

    private void parseAndProcess(File xmlFile) {
        try {
            JAXBContext context = JAXBContext.newInstance(NLSFeed.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            NLSFeed feed = (NLSFeed) unmarshaller.unmarshal(xmlFile);

            if (feed.getNotification() != null) {
                processNotification(feed.getNotification());
            }
            if (feed.getFeedback() != null) {
                processFeedback(feed.getFeedback());
            }
        } catch (Exception e) {
            System.err.println("Failed to parse XML: " + xmlFile.getName());
            e.printStackTrace();
        }
    }

    private void processNotification(Notification notification) {
        System.out.println("Received Notification. Status: " + notification.getMeta().getStat());
        // Update batch status in DB
    }

    private void processFeedback(Feedback feedback) {
        System.out.println("Received Feedback. Records: " + feedback.getMeta().getNor());
        // Update case/party status in DB
    }
}
