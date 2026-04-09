package com.venus.kyc.viewer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * REST client for calling the screening-service batch endpoints from the viewer.
 * Used by the nightly material-change batch job to create and process screening batches.
 */
@Service
public class ScreeningBatchClient {

    private static final Logger log = LoggerFactory.getLogger(ScreeningBatchClient.class);

    private final RestClient restClient;
    private final String batchBaseUrl;

    public ScreeningBatchClient(
            @Value("${screening.service.url}") String screeningServiceUrl,
            @Value("${internal.api.key}") String internalApiKey,
            RestClient.Builder restClientBuilder) {
        // screening.service.url = http://localhost:8082/api/internal/screening
        this.batchBaseUrl = screeningServiceUrl + "/batch";
        this.restClient = restClientBuilder
                .defaultHeader("X-Internal-Api-Key", internalApiKey)
                .build();
    }

    /**
     * Creates a batch with the given clients and runs all pipeline steps (XML → checksum → zip → encrypt → upload).
     *
     * @return the batch ID from the screening service
     */
    public Long createAndProcessBatch(List<Map<String, Object>> clients, String source, String createdBy) {
        // Step 1: Create batch
        log.info("Creating screening batch with {} clients (source={})...", clients.size(), source);
        String batchIdStr = restClient.post()
                .uri(batchBaseUrl + "/create?source={source}&createdBy={createdBy}", source, createdBy)
                .contentType(MediaType.APPLICATION_JSON)
                .body(clients)
                .retrieve()
                .body(String.class);

        Long batchId = Long.parseLong(batchIdStr.trim());
        log.info("Batch created with ID: {}", batchId);

        // Step 2: Generate XML
        log.info("Generating XML for batch {}...", batchId);
        restClient.post()
                .uri(batchBaseUrl + "/{batchId}/generate-xml", batchId)
                .retrieve()
                .body(String.class);

        // Step 3: Generate checksum
        log.info("Generating checksum for batch {}...", batchId);
        restClient.post()
                .uri(batchBaseUrl + "/{batchId}/generate-checksum", batchId)
                .retrieve()
                .body(String.class);

        // Step 4: Zip
        log.info("Zipping files for batch {}...", batchId);
        restClient.post()
                .uri(batchBaseUrl + "/{batchId}/zip", batchId)
                .retrieve()
                .body(String.class);

        // Step 5: Encrypt
        log.info("Encrypting batch {}...", batchId);
        restClient.post()
                .uri(batchBaseUrl + "/{batchId}/encrypt", batchId)
                .retrieve()
                .body(String.class);

        // Step 6: Upload to SFTP
        log.info("Uploading batch {} to SFTP...", batchId);
        restClient.post()
                .uri(batchBaseUrl + "/{batchId}/upload", batchId)
                .retrieve()
                .body(String.class);

        log.info("Batch {} fully processed and uploaded.", batchId);
        return batchId;
    }
}
