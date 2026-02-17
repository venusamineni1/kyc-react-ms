package com.venus.kyc.risk.batch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.venus.kyc.risk.batch.model.Client;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.time.LocalDateTime;

@Service
public class BatchRiskService {

    private final RiskMappingRepository mappingRepository;
    private final ObjectMapper objectMapper;
    private final SftpService sftpService;
    private final CompressionService compressionService;

    @Value("${batch.work.dir:/tmp/risk-batch}")
    private String workDir;

    @Value("${batch.sftp.upload.dir:upload}")
    private String sftpUploadDir;

    private final BatchRepository batchRepository;

    public BatchRiskService(RiskMappingRepository mappingRepository, ObjectMapper objectMapper,
            SftpService sftpService, CompressionService compressionService, BatchRepository batchRepository) {
        this.mappingRepository = mappingRepository;
        this.objectMapper = objectMapper;
        this.sftpService = sftpService;
        this.compressionService = compressionService;
        this.batchRepository = batchRepository;
    }

    public String generateTestJson(Client client) throws Exception {
        ObjectNode root = createClientRequest(client, mappingRepository.findAll());
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
    }

    public String createBatch(List<Client> clients) throws Exception {
        String batchTimestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String batchBaseName = "RISK_BATCH_" + batchTimestamp;

        File batchDir = new File(workDir, batchBaseName);
        if (!batchDir.exists()) {
            batchDir.mkdirs();
        }

        // Save clients selection for later steps
        File clientsFile = new File(batchDir, "selected_clients.json");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(clientsFile, clients);

        // Save Batch Run to DB
        BatchRun run = new BatchRun(null, batchBaseName, "INITIATED", "PENDING", 0, LocalDateTime.now(),
                LocalDateTime.now());
        batchRepository.saveBatchRun(run);

        return batchBaseName;
    }

    public List<BatchRun> getBatchHistory() {
        return batchRepository.findAll();
    }

    public void generateBatchJsonl(String batchId) throws Exception {
        File batchDir = new File(workDir, batchId);
        if (!batchDir.exists())
            throw new RuntimeException("Batch not found: " + batchId);

        File clientsFile = new File(batchDir, "selected_clients.json");
        List<Client> clients = objectMapper.readValue(clientsFile,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Client.class));

        File jsonlFile = new File(batchDir, "clients.jsonl");
        List<RiskMapping> mappings = mappingRepository.findAll();

        try (PrintWriter writer = new PrintWriter(new FileWriter(jsonlFile))) {
            for (Client client : clients) {
                ObjectNode clientJson = createClientRequest(client, mappings);
                writer.println(objectMapper.writeValueAsString(clientJson));
            }
        }
    }

    public void zipBatch(String batchId) throws Exception {
        File batchDir = new File(workDir, batchId);
        File jsonlFile = new File(batchDir, "clients.jsonl");
        File zipFile = new File(batchDir, batchId + ".zip");
        compressionService.zipFiles(Collections.singletonList(jsonlFile), zipFile);
    }

    public void generateControlFile(String batchId) throws Exception {
        File batchDir = new File(workDir, batchId);
        File zipFile = new File(batchDir, batchId + ".zip");

        // 3. Generate Checksum of Zip
        String checksum = calculateChecksum(zipFile);

        File clientsFile = new File(batchDir, "selected_clients.json");
        List<Client> clients = objectMapper.readValue(clientsFile,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Client.class));

        // 4. Generate Control File
        File controlFile = new File(batchDir, "control.json");
        ObjectNode controlJson = objectMapper.createObjectNode();
        controlJson.put("requestFilename", zipFile.getName());
        controlJson.put("totalNoOfRequests", clients.size());
        controlJson.put("checkSum", checksum);
        controlJson.put("requestTimeStamp", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(new Date()));
        controlJson.put("callerSystem", "36073-1");
        controlJson.put("mode", "CRRE");
        controlJson.put("processType", "batch");

        objectMapper.writerWithDefaultPrettyPrinter().writeValue(controlFile, controlJson);
    }

    public void uploadBatch(String batchId) throws Exception {
        File batchDir = new File(workDir, batchId);
        File zipFile = new File(batchDir, batchId + ".zip");
        File controlFile = new File(batchDir, "control.json");

        // 5. Send both json zip file and control file via sftp
        sftpService.uploadFile(zipFile, sftpUploadDir);
        sftpService.uploadFile(controlFile, sftpUploadDir);
    }

    public String initiateBatch(List<Client> clients) throws Exception {
        String batchId = createBatch(clients);
        generateBatchJsonl(batchId);
        zipBatch(batchId);
        generateControlFile(batchId);
        uploadBatch(batchId);
        return batchId;
    }

    private String calculateChecksum(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream fis = new FileInputStream(file)) {
            byte[] byteArray = new byte[1024];
            int bytesCount;
            while ((bytesCount = fis.read(byteArray)) != -1) {
                digest.update(byteArray, 0, bytesCount);
            }
        }
        byte[] bytes = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private ObjectNode createClientRequest(Client client, List<RiskMapping> mappings) {
        ObjectNode root = objectMapper.createObjectNode();

        // Header
        ObjectNode header = root.putObject("header");
        header.put("callerSystem", "173471-1");
        header.put("requestID", "REQ-" + System.currentTimeMillis() + "-" + client.clientID()); // Unique ID
        header.put("dbBusinessline", "WM");
        header.put("requestTimeStamp", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(new Date()));
        header.put("crrmVersion", "2.0");

        ArrayNode requests = root.putArray("clientRiskRatingRequest");
        ObjectNode clientData = objectMapper.createObjectNode();
        requests.add(clientData);

        // Initialize sections
        clientData.putObject("clientDetails");
        clientData.putObject("entityRiskType");
        clientData.putObject("industryRiskType");
        clientData.putObject("geoRiskType");
        clientData.putArray("productRiskType");
        clientData.putObject("channelRiskType");

        // Defaults required by schema
        ((ObjectNode) clientData.get("clientDetails")).putArray("additionalRule");

        for (RiskMapping mapping : mappings) {
            String value = getValueFromClient(client, mapping);
            if (value == null)
                value = mapping.defaultValue();

            if (value != null) {
                setMappedValue(clientData, mapping.targetPath(), value);
            }
        }

        return root;
    }

    private String getValueFromClient(Client client, RiskMapping mapping) {
        if (mapping.sourceField() == null)
            return null;
        try {
            java.lang.reflect.Method method = Client.class.getMethod(mapping.sourceField());
            Object result = method.invoke(client);
            return result != null ? String.valueOf(result) : null;
        } catch (Exception e) {
            return null;
        }
    }

    public String getFileContent(String batchName, String fileType) throws IOException {
        File batchDir = new File(workDir, batchName);
        File file = null;
        switch (fileType) {
            case "jsonl":
                file = new File(batchDir, "clients.jsonl");
                break;
            case "control":
                file = new File(batchDir, "control.json");
                break;
            default:
                return "Unsupported file type";
        }

        if (file.exists()) {
            return Files.readString(file.toPath(), StandardCharsets.UTF_8);
        }
        return "File not found";
    }

    private void setMappedValue(ObjectNode request, String targetPath, String value) {
        String[] parts = targetPath.split("\\.");

        // Navigate to the correct section based on targetPath
        // Example path: clientDetails.recordID
        if (targetPath.startsWith("clientDetails.")) {
            ((ObjectNode) request.get("clientDetails")).put(parts[1], value);
        } else if (targetPath.startsWith("entityRiskType.")) {
            ((ObjectNode) request.get("entityRiskType")).put(parts[1], value);
        } else if (targetPath.startsWith("industryRiskType.")) {
            ((ObjectNode) request.get("industryRiskType")).put(parts[1], value);
        } else if (targetPath.startsWith("geoRiskType.")) {
            if (parts.length > 2 && parts[1].equals("addressType")) {
                ObjectNode addr = (ObjectNode) ((ObjectNode) request.get("geoRiskType")).get("addressType");
                if (addr == null)
                    addr = ((ObjectNode) request.get("geoRiskType")).putObject("addressType");
                addr.put(parts[2], value);
            } else {
                ((ObjectNode) request.get("geoRiskType")).put(parts[1], value);
            }
        } else if (targetPath.startsWith("channelRiskType.")) {
            ((ObjectNode) request.get("channelRiskType")).put(parts[1], value);
        } else if (targetPath.startsWith("productRiskType.")) {
            ArrayNode arr = (ArrayNode) request.get("productRiskType");
            ObjectNode item;
            if (arr.size() == 0)
                item = arr.addObject();
            else
                item = (ObjectNode) arr.get(0);
            item.put(parts[1], value);
        }
    }
}
