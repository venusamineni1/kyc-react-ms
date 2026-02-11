package com.venus.kyc.risk.batch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.venus.kyc.risk.batch.model.Client;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Service
public class BatchRiskService {

    private final RiskMappingRepository mappingRepository;
    private final ObjectMapper objectMapper;

    @Value("${batch.work.dir:/tmp/risk-batch}")
    private String workDir;

    public BatchRiskService(RiskMappingRepository mappingRepository, ObjectMapper objectMapper) {
        this.mappingRepository = mappingRepository;
        this.objectMapper = objectMapper;
    }

    public String generateTestJson(Client client) throws Exception {
        ObjectNode root = createRiskPayload(List.of(client));
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
    }

    public File initiateBatch(List<Client> clients) throws Exception {
        String batchTimestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String fileName = "RISK_BATCH_" + batchTimestamp + ".json";

        File batchDir = new File(workDir);
        batchDir.mkdirs();

        ObjectNode root = createRiskPayload(clients);
        String jsonContent = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);

        File outputFile = new File(batchDir, fileName);
        Files.writeString(outputFile.toPath(), jsonContent, StandardCharsets.UTF_8);

        return outputFile;
    }

    private ObjectNode createRiskPayload(List<Client> clients) {
        ObjectNode root = objectMapper.createObjectNode();

        // Header
        ObjectNode header = root.putObject("header");
        header.put("callerSystem", "173471-1");
        header.put("requestID", "REQ-" + System.currentTimeMillis());
        header.put("dbBusinessline", "WM");
        header.put("requestTimeStamp", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(new Date()));
        header.put("crrmVersion", "2.0");

        ArrayNode requests = root.putArray("clientRiskRatingRequest");
        List<RiskMapping> mappings = mappingRepository.findAll();

        for (Client client : clients) {
            requests.add(createClientRequest(client, mappings));
        }

        return root;
    }

    private ObjectNode createClientRequest(Client client, List<RiskMapping> mappings) {
        ObjectNode request = objectMapper.createObjectNode();

        // Initialize sections
        request.putObject("clientDetails");
        request.putObject("entityRiskType");
        request.putObject("industryRiskType");
        request.putObject("geoRiskType");
        request.putArray("productRiskType");
        request.putObject("channelRiskType");

        // Defaults required by schema
        ((ObjectNode) request.get("clientDetails")).putArray("additionalRule");

        for (RiskMapping mapping : mappings) {
            String value = getValueFromClient(client, mapping);
            if (value == null)
                value = mapping.defaultValue();

            if (value != null) {
                setMappedValue(request, mapping.targetPath(), value);
            }
        }

        return request;
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
