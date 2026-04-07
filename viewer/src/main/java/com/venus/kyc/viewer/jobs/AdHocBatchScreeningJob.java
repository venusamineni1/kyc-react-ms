package com.venus.kyc.viewer.jobs;

import com.venus.kyc.viewer.*;
import com.venus.kyc.viewer.service.ScreeningBatchClient;
import org.jobrunr.jobs.annotations.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * One-off job that submits a hand-picked list of clients through the full
 * batch screening pipeline. Enqueued via POST /api/internal/scheduler/jobs/enqueue-batch-screening.
 */
@Component
public class AdHocBatchScreeningJob {

    private static final Logger log = LoggerFactory.getLogger(AdHocBatchScreeningJob.class);

    private final ClientRepository clientRepository;
    private final ScreeningBatchClient screeningBatchClient;

    public AdHocBatchScreeningJob(ClientRepository clientRepository,
                                   ScreeningBatchClient screeningBatchClient) {
        this.clientRepository = clientRepository;
        this.screeningBatchClient = screeningBatchClient;
    }

    @Job(name = "Ad-Hoc Client Screening Batch")
    public void execute(List<Long> clientIds, String createdBy) {
        log.info("[AD-HOC BATCH] Screening batch started for {} client(s), createdBy={}",
                clientIds.size(), createdBy);

        List<Map<String, Object>> mappedClients = new ArrayList<>();

        for (Long clientId : clientIds) {
            Optional<Client> clientOpt = clientRepository.findById(clientId);
            if (clientOpt.isEmpty()) {
                log.warn("[AD-HOC BATCH] Client {} not found, skipping", clientId);
                continue;
            }
            mappedClients.add(mapClientForScreening(clientOpt.get()));
        }

        if (mappedClients.isEmpty()) {
            log.warn("[AD-HOC BATCH] No valid clients found. Skipping batch creation.");
            return;
        }

        try {
            Long batchId = screeningBatchClient.createAndProcessBatch(mappedClients, "AD_HOC", createdBy);
            log.info("[AD-HOC BATCH] Batch {} created and uploaded with {} client(s)",
                    batchId, mappedClients.size());
        } catch (Exception e) {
            log.error("[AD-HOC BATCH] Failed: {}", e.getMessage(), e);
            throw new RuntimeException("Ad-Hoc Screening Batch failed: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> mapClientForScreening(Client client) {
        Map<String, Object> m = new LinkedHashMap<>();

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
