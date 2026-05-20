package com.venus.kyc.risk.crre;

import com.venus.kyc.risk.RiskDTOs;
import com.venus.kyc.risk.RiskProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Real CRRE implementation of RiskProvider.
 * Delegates HTTP call (with mTLS) to CrreHttpClient.
 * Active when crre.mock=false.
 */
@Component
@ConditionalOnProperty(name = "crre.mock", havingValue = "false")
public class CrreRiskProvider implements RiskProvider {

    private static final Logger log = LoggerFactory.getLogger(CrreRiskProvider.class);

    private final CrreHttpClient httpClient;

    public CrreRiskProvider(CrreHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public RiskDTOs.CalculateRiskResponse calculateRisk(RiskDTOs.CalculateRiskRequest request) {
        String recordId = extractRecordId(request);
        log.info("Calling real CRRE API for recordId={}", recordId);

        RiskDTOs.CalculateRiskResponse response = httpClient.calculateRisk(request);

        log.info("CRRE response received for recordId={} — status={}",
                recordId,
                response.processStatus() != null ? response.processStatus().crreStatus() : "unknown");

        return response;
    }

    private String extractRecordId(RiskDTOs.CalculateRiskRequest request) {
        try {
            return request.clientRiskRatingRequest().get(0).clientDetails().recordID();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
