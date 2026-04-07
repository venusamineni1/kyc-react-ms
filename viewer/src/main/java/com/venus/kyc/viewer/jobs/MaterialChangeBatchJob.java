package com.venus.kyc.viewer.jobs;

import com.venus.kyc.viewer.*;
import com.venus.kyc.viewer.service.ScreeningBatchClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Nightly job that collects all pending material changes categorized for screening,
 * maps the affected clients to the screening-service batch format,
 * and submits them as a screening batch file.
 */
@Component
public class MaterialChangeBatchJob {

    private static final Logger log = LoggerFactory.getLogger(MaterialChangeBatchJob.class);

    private final MaterialChangeRepository materialChangeRepository;
    private final ClientRepository clientRepository;
    private final ScreeningBatchClient screeningBatchClient;

    public MaterialChangeBatchJob(MaterialChangeRepository materialChangeRepository,
            ClientRepository clientRepository, ScreeningBatchClient screeningBatchClient) {
        this.materialChangeRepository = materialChangeRepository;
        this.clientRepository = clientRepository;
        this.screeningBatchClient = screeningBatchClient;
    }

    public void execute() {
        log.info("[BATCH JOB] Material Change → Screening Batch started");

        // 1. Find all pending screening changes
        List<MaterialChange> pendingChanges = materialChangeRepository.findPendingScreeningChanges();
        if (pendingChanges.isEmpty()) {
            log.info("[BATCH JOB] No pending screening material changes found. Skipping.");
            return;
        }

        log.info("[BATCH JOB] Found {} pending screening changes", pendingChanges.size());

        // 2. Collect distinct client IDs
        Set<Long> clientIds = pendingChanges.stream()
                .map(MaterialChange::clientID)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        log.info("[BATCH JOB] {} distinct clients affected", clientIds.size());

        // 3. Fetch full client data and map to screening format
        List<Map<String, Object>> mappedClients = new ArrayList<>();
        List<Long> processedClientIds = new ArrayList<>();

        for (Long clientId : clientIds) {
            Optional<Client> clientOpt = clientRepository.findById(clientId);
            if (clientOpt.isEmpty()) {
                log.warn("[BATCH JOB] Client {} not found, skipping", clientId);
                continue;
            }

            Client client = clientOpt.get();
            Map<String, Object> mapped = mapClientForScreening(client);
            mappedClients.add(mapped);
            processedClientIds.add(clientId);
        }

        if (mappedClients.isEmpty()) {
            log.warn("[BATCH JOB] No valid clients found for screening. Skipping batch creation.");
            return;
        }

        // 4. Call screening service to create and process batch
        try {
            Long batchId = screeningBatchClient.createAndProcessBatch(
                    mappedClients, "BATCH_JOB", "SYSTEM");

            log.info("[BATCH JOB] Screening batch {} created and uploaded with {} clients",
                    batchId, mappedClients.size());

            // 5. Update material change statuses to BATCH_SUBMITTED
            List<Long> changeIds = pendingChanges.stream()
                    .filter(mc -> processedClientIds.contains(mc.clientID()))
                    .map(MaterialChange::changeID)
                    .collect(Collectors.toList());

            materialChangeRepository.updateStatusBatch(changeIds, "BATCH_SUBMITTED");
            log.info("[BATCH JOB] Updated {} material changes to BATCH_SUBMITTED", changeIds.size());

        } catch (Exception e) {
            log.error("[BATCH JOB] Failed to create screening batch: {}", e.getMessage(), e);
            // Changes remain PENDING — will be retried on next run
            throw new RuntimeException("Material Change Batch Job failed: " + e.getMessage(), e);
        }

        log.info("[BATCH JOB] Material Change → Screening Batch completed successfully");
    }

    /**
     * Maps a viewer Client to the flat Map format expected by the screening-service.
     * Mirrors the same mapping logic from BatchPipeline.jsx on the frontend.
     */
    private Map<String, Object> mapClientForScreening(Client client) {
        Map<String, Object> m = new LinkedHashMap<>();

        // Core identity fields
        m.put("clientID", client.clientID());
        m.put("titlePrefix", client.titlePrefix());
        m.put("firstName", client.firstName());
        m.put("middleName", client.middleName());
        m.put("lastName", client.lastName());
        m.put("titleSuffix", client.titleSuffix());
        m.put("citizenship1", client.citizenship1());
        m.put("citizenship2", client.citizenship2());
        m.put("onboardingDate", client.onboardingDate() != null ? client.onboardingDate().toString() : null);
        m.put("dateOfBirth", client.dateOfBirth() != null ? client.dateOfBirth().toString() : null);
        m.put("status", client.status());
        m.put("nameAtBirth", client.nameAtBirth());
        m.put("nickName", client.nickName());
        m.put("gender", client.gender());
        m.put("language", client.language());
        m.put("occupation", client.occupation());
        m.put("countryOfTax", client.countryOfTax());
        m.put("sourceOfFundsCountry", client.sourceOfFundsCountry());
        m.put("fatcaStatus", client.fatcaStatus());
        m.put("crsStatus", client.crsStatus());
        m.put("placeOfBirth", client.placeOfBirth());
        m.put("cityOfBirth", client.cityOfBirth());
        m.put("countryOfBirth", client.countryOfBirth());

        // Address flattening (first address)
        List<Address> addresses = client.addresses();
        if (addresses != null && !addresses.isEmpty()) {
            Address addr = addresses.get(0);
            m.put("addressLine1", addr.addressLine1());
            m.put("city", addr.city());
            m.put("zipCode", addr.zip());
            m.put("province", addr.addressSupplement() != null ? addr.addressSupplement() : "");
            m.put("country", addr.country() != null ? addr.country()
                    : (client.citizenship1() != null ? client.citizenship1()
                            : (client.countryOfTax() != null ? client.countryOfTax() : "US")));
        } else {
            m.put("country", client.citizenship1() != null ? client.citizenship1()
                    : (client.countryOfTax() != null ? client.countryOfTax() : "US"));
        }

        // Identifier flattening (first identifier)
        List<Identifier> identifiers = client.identifiers();
        if (identifiers != null && !identifiers.isEmpty()) {
            Identifier id = identifiers.get(0);
            m.put("nationality", client.citizenship1());
            m.put("legDocType", id.identifierType());
            m.put("idNumber", id.identifierValue() != null ? id.identifierValue() : id.identifierNumber());
        } else {
            m.put("nationality", client.citizenship1());
        }

        return m;
    }
}
