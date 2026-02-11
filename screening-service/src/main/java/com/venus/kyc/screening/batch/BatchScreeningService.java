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
    private final MappingConfigRepository mappingConfigRepository;

    public BatchScreeningService(EncryptionService encryptionService, CompressionService compressionService,
            SftpService sftpService, BatchRepository batchRepository, MappingConfigRepository mappingConfigRepository) {
        this.encryptionService = encryptionService;
        this.compressionService = compressionService;
        this.sftpService = sftpService;
        this.batchRepository = batchRepository;
        this.mappingConfigRepository = mappingConfigRepository;
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

    public String generateTestXml(Client client) throws Exception {
        String batchName = "TEST_BATCH_" + System.currentTimeMillis();
        NLSFeed feed = createFeed(batchName, List.of(client));

        JAXBContext context = JAXBContext.newInstance(NLSFeed.class);
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

        StringWriter sw = new StringWriter();
        marshaller.marshal(feed, sw);
        return sw.toString();
    }

    private NLSFeed createFeed(String batchName, List<Client> clients) {
        NLSFeed feed = new NLSFeed();
        Request request = new Request();

        // Meta
        RequestMeta meta = new RequestMeta();
        meta.setSrcId("2475");
        meta.setTor("RCDelta");
        meta.setCrtTm(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").format(new Date()));
        meta.setAod(new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        meta.setNor(clients.size());

        FInfo fInfo = new FInfo();
        fInfo.setName(batchName);
        fInfo.setBatchNr("1"); // Incremental?
        fInfo.setfBatch("N");
        fInfo.setCntr("US");
        fInfo.setBd("20250101");
        fInfo.setLbj("US");
        fInfo.setLafcj("US");
        fInfo.setBsrl("US");
        meta.setfInfo(fInfo);

        request.setMeta(meta);

        // Records
        Records records = new Records();
        List<com.venus.kyc.screening.batch.model.Record> recList = new ArrayList<>();

        List<MappingConfig> mappingConfigs = mappingConfigRepository.findAll();
        if (mappingConfigs.isEmpty()) {
            mappingConfigs = getDefaultMappings();
        }

        for (Client client : clients) {
            recList.add(createRecord(client, mappingConfigs));
        }
        records.setRecList(recList);
        request.setRecords(records);

        feed.setRequest(request);
        return feed;
    }

    private List<MappingConfig> getDefaultMappings() {
        return List.of(
                new MappingConfig(null, "record.uniRcrdId", "clientID", null, null),
                new MappingConfig(null, "record.type", null, "PC", null),
                new MappingConfig(null, "record.recStat", null, "M", null),
                new MappingConfig(null, "name.full", "fullName", null, null),
                new MappingConfig(null, "name.type", null, "PN", null),
                new MappingConfig(null, "name.fir", "firstName", "Unknown", null),
                new MappingConfig(null, "name.mid", "middleName", "", null),
                new MappingConfig(null, "name.sur", "lastName", "Unknown", null),
                new MappingConfig(null, "name.ma", "maidenName", "", null),
                new MappingConfig(null, "individual.gender", "gender", "U", null),
                new MappingConfig(null, "individual.dob", "dateOfBirth", null, null),
                new MappingConfig(null, "individual.cntr", "residenceCountry", "US", null),
                new MappingConfig(null, "individual.placeOfBirth", "residenceCountry", "Unknown", null),
                new MappingConfig(null, "individual.occupation", "occupation", "Unknown", null));
    }

    private com.venus.kyc.screening.batch.model.Record createRecord(Client client, List<MappingConfig> mappings) {
        com.venus.kyc.screening.batch.model.Record record = new com.venus.kyc.screening.batch.model.Record();
        RecordMeta meta = new RecordMeta();
        RecordData data = new RecordData();
        PartyInfo info = new PartyInfo();
        Individual ind = new Individual();
        Names names = new Names();
        Name n = new Name();

        record.setMeta(meta);
        record.setData(data);
        data.setPrtInfo(info);

        JuridicalInfo juri = new JuridicalInfo();
        List<BUInfo> bus = new ArrayList<>();
        BUInfo bu = new BUInfo();
        bu.setRelSrcId("2475");
        bu.setRecCntrOrg("US");
        bu.setLbj("US");
        bu.setLafcj("US");
        bu.setBsrl("US");
        bus.add(bu);
        juri.setBu(bus);
        data.setJuriInfo(juri);

        info.setInd(ind);
        ind.setNames(names);
        List<Name> nameList = new ArrayList<>();
        nameList.add(n);
        names.setNameList(nameList);

        // Mandatory Address
        Addresses addrs = new Addresses();
        List<Address> addrList = new ArrayList<>();
        Address addr = new Address();
        addr.setType("Residential");
        addr.setLine("Unknown Line 1");
        addr.setCity("Unknown City");
        addr.setZipCode("00000");
        addr.setProv("Unknown Prov");
        addr.setCntr("US");
        addrList.add(addr);
        addrs.setAddrList(addrList);
        ind.setAddresses(addrs);

        for (MappingConfig config : mappings) {
            String value = getValueFromClient(client, config);
            if (value == null)
                value = config.defaultValue();

            if (value != null) {
                setMappedValue(record, n, ind, meta, data, config.targetPath(), value);
            }
        }

        // Calculate Checksum after all fields are populated
        meta.setChkSum(calculateRecordChecksum(data));

        return record;
    }

    private String calculateRecordChecksum(RecordData data) {
        try {
            StringBuilder sb = new StringBuilder();
            collectValues(data, sb);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString().toUpperCase();
        } catch (Exception e) {
            return "";
        }
    }

    private void collectValues(Object obj, StringBuilder sb) {
        if (obj == null)
            return;
        if (obj instanceof String) {
            sb.append(obj);
            return;
        }
        if (obj instanceof List) {
            for (Object item : (List<?>) obj) {
                collectValues(item, sb);
            }
            return;
        }
        // Use reflection to get all fields recursively
        for (java.lang.reflect.Field field : obj.getClass().getDeclaredFields()) {
            try {
                field.setAccessible(true);
                Object val = field.get(obj);
                if (val != null) {
                    collectValues(val, sb);
                }
            } catch (Exception e) {
                // ignore
            }
        }
    }

    private String getValueFromClient(Client client, MappingConfig config) {
        if (config.sourceField() == null)
            return null;

        // Special logic for fullName
        if ("fullName".equals(config.sourceField())) {
            StringBuilder fullName = new StringBuilder();
            if (client.firstName() != null)
                fullName.append(client.firstName()).append(" ");
            if (client.middleName() != null && !client.middleName().isEmpty())
                fullName.append(client.middleName()).append(" ");
            if (client.lastName() != null)
                fullName.append(client.lastName());
            return fullName.toString().trim();
        }

        try {
            java.lang.reflect.Method method = Client.class.getMethod(config.sourceField());
            Object result = method.invoke(client);
            return result != null ? String.valueOf(result) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private void setMappedValue(com.venus.kyc.screening.batch.model.Record record, Name n, Individual ind,
            RecordMeta meta, RecordData data, String targetPath, String value) {
        switch (targetPath) {
            case "record.uniRcrdId":
                meta.setUniRcrdId(value);
                break;
            case "record.type":
                meta.setType(value);
                break;
            case "record.recStat":
                meta.setRecStat(value);
                break;
            case "name.full":
                n.setFull(value);
                break;
            case "name.type":
                n.setType(value);
                break;
            case "name.tit":
                n.setTit(value);
                break;
            case "name.fir":
                n.setFir(value);
                break;
            case "name.mid":
                n.setMid(value);
                break;
            case "name.sur":
                n.setSur(value);
                break;
            case "name.ma":
                n.setMa(value);
                break;
            case "individual.gender":
                ind.setGender(value);
                break;
            case "individual.dob":
                ind.setDob(value);
                break;
            case "individual.placeOfBirth":
                ind.setPlaceOfBirth(value);
                break;
            case "individual.cntr":
                ind.setCntr(value);
                break;
            case "individual.occupation":
                ind.setOccupation(value);
                break;
            case "comment":
                data.setComment(value);
                break;
            // Simplified: first entry for lists
            case "individual.nationality":
                if (ind.getNationalities() == null)
                    ind.setNationalities(new Nationalities());
                if (ind.getNationalities().getNatList() == null)
                    ind.getNationalities().setNatList(new ArrayList<>());
                if (ind.getNationalities().getNatList().isEmpty())
                    ind.getNationalities().getNatList().add(new Nationality());
                ind.getNationalities().getNatList().get(0).setCntr(value);
                break;
            case "individual.address":
            case "individual.address.line":
                if (ind.getAddresses() == null)
                    ind.setAddresses(new Addresses());
                if (ind.getAddresses().getAddrList() == null)
                    ind.getAddresses().setAddrList(new ArrayList<>());
                if (ind.getAddresses().getAddrList().isEmpty())
                    ind.getAddresses().getAddrList().add(new Address());
                ind.getAddresses().getAddrList().get(0).setLine(value);
                break;
            case "individual.address.city":
                if (ind.getAddresses() == null)
                    ind.setAddresses(new Addresses());
                if (ind.getAddresses().getAddrList() == null)
                    ind.getAddresses().setAddrList(new ArrayList<>());
                if (ind.getAddresses().getAddrList().isEmpty())
                    ind.getAddresses().getAddrList().add(new Address());
                ind.getAddresses().getAddrList().get(0).setCity(value);
                break;
            case "individual.address.zip":
                if (ind.getAddresses() == null)
                    ind.setAddresses(new Addresses());
                if (ind.getAddresses().getAddrList() == null)
                    ind.getAddresses().setAddrList(new ArrayList<>());
                if (ind.getAddresses().getAddrList().isEmpty())
                    ind.getAddresses().getAddrList().add(new Address());
                ind.getAddresses().getAddrList().get(0).setZipCode(value);
                break;
            case "individual.address.prov":
                if (ind.getAddresses() == null)
                    ind.setAddresses(new Addresses());
                if (ind.getAddresses().getAddrList() == null)
                    ind.getAddresses().setAddrList(new ArrayList<>());
                if (ind.getAddresses().getAddrList().isEmpty())
                    ind.getAddresses().getAddrList().add(new Address());
                ind.getAddresses().getAddrList().get(0).setProv(value);
                break;
            case "individual.address.cntr":
                if (ind.getAddresses() == null)
                    ind.setAddresses(new Addresses());
                if (ind.getAddresses().getAddrList() == null)
                    ind.getAddresses().setAddrList(new ArrayList<>());
                if (ind.getAddresses().getAddrList().isEmpty())
                    ind.getAddresses().getAddrList().add(new Address());
                ind.getAddresses().getAddrList().get(0).setCntr(value);
                break;
            case "account.nr":
                if (data.getPrtInfo().getAccount() == null)
                    data.getPrtInfo().setAccount(new Account());
                data.getPrtInfo().getAccount().setNr(value);
                break;
            case "kyc.pepFlag":
                if (data.getKycData() == null)
                    data.setKycData(new KYCData());
                data.getKycData().setPepFlag(value);
                break;

            case "individual.nationality.legDoc":
                if (ind.getNationalities() == null)
                    ind.setNationalities(new Nationalities());
                if (ind.getNationalities().getNatList() == null)
                    ind.getNationalities().setNatList(new ArrayList<>());
                if (ind.getNationalities().getNatList().isEmpty())
                    ind.getNationalities().getNatList().add(new Nationality());
                ind.getNationalities().getNatList().get(0).setLegDoc(value);
                break;
            case "individual.nationality.idNr":
                if (ind.getNationalities() == null)
                    ind.setNationalities(new Nationalities());
                if (ind.getNationalities().getNatList() == null)
                    ind.getNationalities().setNatList(new ArrayList<>());
                if (ind.getNationalities().getNatList().isEmpty())
                    ind.getNationalities().getNatList().add(new Nationality());
                ind.getNationalities().getNatList().get(0).setIdNr(value);
                break;
        }
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
