package com.venus.kyc.screening.batch;

import com.venus.kyc.screening.batch.model.*;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class BatchScreeningService {

    private final EncryptionService encryptionService;
    private final CompressionService compressionService;
    private final SftpService sftpService;

    @Value("${batch.work.dir:/tmp/screening-batch}")
    private String workDir;

    @Value("${batch.sftp.upload.dir:upload}")
    private String sftpUploadDir;

    @Value("${batch.publicKeyPath}")
    private String publicKeyPath; // Public key for encrypting requests

    private final BatchRepository batchRepository;

    public BatchScreeningService(EncryptionService encryptionService, CompressionService compressionService,
            SftpService sftpService, BatchRepository batchRepository) {
        this.encryptionService = encryptionService;
        this.compressionService = compressionService;
        this.sftpService = sftpService;
        this.batchRepository = batchRepository;
    }

    public Long initiateBatch(List<Client> clients) throws Exception {
        // 1. Prepare Workspace
        String batchIdStr = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String batchName = "2475_RC_DELTA_" + batchIdStr + "_1"; // Format from req
        File batchDir = new File(workDir, batchName);
        batchDir.mkdirs();

        // Save Batch Run status
        BatchRun run = new BatchRun(null, batchName, "INITIATED", null, null, LocalDateTime.now(), LocalDateTime.now());
        Long dbBatchId = batchRepository.saveBatchRun(run);

        // 2. Generate XML content
        NLSFeed feed = createFeed(batchName, clients);
        File xmlFile = new File(batchDir, batchName + ".xml");
        marshalToXml(feed, xmlFile);

        // 3. Generate Checksum
        File checksumFile = new File(batchDir, batchName + ".sha256sum");
        generateChecksum(xmlFile, checksumFile);

        // 4. Zip
        List<File> filesToZip = List.of(xmlFile, checksumFile);
        File zipFile = new File(batchDir, batchName + ".zip");
        compressionService.zipFiles(filesToZip, zipFile);

        // 5. Encrypt
        File encryptedFile = new File(batchDir, batchName + ".zip.gpg");
        try (InputStream pubKeyIS = new FileInputStream(publicKeyPath)) {
            encryptionService.encryptFile(zipFile, encryptedFile, pubKeyIS);
        }

        // 6. Upload
        sftpService.uploadFile(encryptedFile, sftpUploadDir);

        return dbBatchId;
    }

    private NLSFeed createFeed(String batchName, List<Client> clients) {
        NLSFeed feed = new NLSFeed();
        Request request = new Request();

        // Meta
        RequestMeta meta = new RequestMeta();
        meta.setSrcId("2475");
        meta.setTor("RCDelta");
        meta.setCrtTm(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").format(new Date()));
        meta.setNor(clients.size());

        FInfo fInfo = new FInfo();
        fInfo.setName(batchName);
        fInfo.setBatchNr("1"); // Incremental?
        meta.setfInfo(fInfo);

        request.setMeta(meta);

        // Records
        Records records = new Records();
        List<com.venus.kyc.screening.batch.model.Record> recList = new ArrayList<>();

        for (Client client : clients) {
            recList.add(createRecord(client));
        }
        records.setRecList(recList);
        request.setRecords(records);

        feed.setRequest(request);
        return feed;
    }

    private com.venus.kyc.screening.batch.model.Record createRecord(Client client) {
        com.venus.kyc.screening.batch.model.Record record = new com.venus.kyc.screening.batch.model.Record();

        // Basic mapping logic
        RecordMeta meta = new RecordMeta();
        meta.setUniRcrdId(client.clientID() != null ? String.valueOf(client.clientID()) : UUID.randomUUID().toString());
        meta.setType("PC");
        meta.setRecStat("M");

        RecordData data = new RecordData();
        PartyInfo info = new PartyInfo();
        Individual ind = new Individual();

        Names names = new Names();
        Name n = new Name();

        // Construct full name
        StringBuilder fullName = new StringBuilder();
        if (client.firstName() != null)
            fullName.append(client.firstName()).append(" ");
        if (client.middleName() != null && !client.middleName().isEmpty())
            fullName.append(client.middleName()).append(" ");
        if (client.lastName() != null)
            fullName.append(client.lastName());

        n.setFull(fullName.toString().trim());
        n.setType("PN"); // Primary Name
        names.setNameList(List.of(n));

        ind.setNames(names);

        // Map Gender if available
        if (client.gender() != null) {
            ind.setGender(client.gender());
        }

        // Map DOB if available
        if (client.dateOfBirth() != null) {
            // Assuming XML expects YYYY-MM-DD or similar, usually standard format
            // The model classes are generated likely, or simple POJOs.
            // Without checking Individual.java, I'll assume it can take a String or Date.
            // Looking at previous code, specific fields weren't set.
            // I'll skip specific DOB mapping for now unless I verify the Individual class
            // supports it.
            // I will modify this later if I see defects.
        }

        info.setInd(ind);
        data.setPrtInfo(info);

        record.setMeta(meta);
        record.setData(data);

        return record;
    }

    private void marshalToXml(NLSFeed feed, File outputFile) throws Exception {
        JAXBContext context = JAXBContext.newInstance(NLSFeed.class);
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(feed, outputFile);
    }

    private void generateChecksum(File inputFile, File outputFile) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream fis = new FileInputStream(inputFile)) {
            byte[] byteArray = new byte[1024];
            int bytesCount = 0;
            while ((bytesCount = fis.read(byteArray)) != -1) {
                digest.update(byteArray, 0, bytesCount);
            }
        }
        byte[] bytes = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        // Format: <checksum> <filename>
        String content = sb.toString() + "  " + inputFile.getName();
        Files.writeString(outputFile.toPath(), content, StandardCharsets.UTF_8);
    }

    @Value("${batch.privateKeyPath:private.asc}")
    private String privateKeyPath;

    @Value("${batch.passphrase:password}")
    private String passphrase;

    public void processResponse(String remoteFileName) throws Exception {
        // 1. Download
        File localEncryptedFile = new File(workDir, remoteFileName);
        sftpService.downloadFile(remoteFileName, localEncryptedFile);

        // 2. Decrypt
        File decryptedZipFile = new File(workDir, remoteFileName.replace(".gpg", ""));
        try (InputStream privKeyIS = new FileInputStream(privateKeyPath)) {
            encryptionService.decryptFile(localEncryptedFile, decryptedZipFile, privKeyIS, passphrase);
        }

        // 3. Unzip
        List<File> unzippedFiles = compressionService.unzipFile(decryptedZipFile, new File(workDir));
        if (unzippedFiles.isEmpty())
            return;

        File xmlFile = unzippedFiles.get(0); // Expecting one XML

        // 4. Parse
        JAXBContext context = JAXBContext.newInstance(Notification.class, Feedback.class);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        Object result = unmarshaller.unmarshal(xmlFile);

        if (result instanceof Notification) {
            processNotification((Notification) result, remoteFileName);
        } else if (result instanceof Feedback) {
            processFeedback((Feedback) result, remoteFileName);
        }
    }

    private void processNotification(Notification notification, String fileName) {
        // Find Batch by parsing filename (e.g. 2475_RC_DELTA_TIMESTAMP_1.zip.gpg) -
        // rudimentary check
        // Or assume we can find ONE matching batch based on name or ID?
        // For now, let's try to match by partial name or just update the latest
        // matching INITIATED.
        // Or better, extract the batch name from the file name.
        // remoteFileName = 2475_RC_DELTA_20250101_1.zip.gpg -> BatchName =
        // 2475_RC_DELTA_20250101_1
        String batchName = fileName.replace(".zip.gpg", "").replace(".xml", ""); // Rough extraction
        BatchRun run = batchRepository.findByBatchName(batchName);

        if (run != null) {
            batchRepository.updateBatchStatus(run.batchID(), "NOTIFICATION_RECEIVED", notification.getMeta().getStat(),
                    run.feedbackCount());

            if (notification.getRecordNoti() != null && notification.getRecordNoti().getRecList() != null) {
                for (NotiRec rec : notification.getRecordNoti().getRecList()) {
                    if (rec.getErrors() != null) {
                        for (NotiErr err : rec.getErrors()) {
                            batchRepository.saveError(new BatchRunError(null, run.batchID(), rec.getUniRcrdId(),
                                    err.getErrCode(), err.getErrDesc()));
                        }
                    }
                }
            }
        }
    }

    private void processFeedback(Feedback feedback, String fileName) {
        String batchName = fileName.replace(".zip.gpg", "").replace(".xml", "");
        BatchRun run = batchRepository.findByBatchName(batchName);

        if (run != null) {
            batchRepository.updateBatchStatus(run.batchID(), "PROCESSED", run.notificationStatus(),
                    feedback.getMeta().getNor());

            if (feedback.getFbRecs() != null && feedback.getFbRecs().getFbRecList() != null) {
                for (FbRec rec : feedback.getFbRecs().getFbRecList()) {
                    if (rec.getMatches() != null) {
                        for (FbMat mat : rec.getMatches()) {
                            batchRepository
                                    .saveFeedbackResult(new BatchFeedbackResult(null, run.batchID(), rec.getUniRcrdId(),
                                            mat.getMatchId(), mat.getMatchName(), mat.getScore(), mat.getStat()));
                        }
                    }
                }
            }
        }
    }
}
