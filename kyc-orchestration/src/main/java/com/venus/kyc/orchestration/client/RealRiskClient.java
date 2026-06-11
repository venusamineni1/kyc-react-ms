package com.venus.kyc.orchestration.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@ConditionalOnProperty(name = "orchestration.risk.mock-enabled", havingValue = "false")
@Slf4j
public class RealRiskClient implements RiskClientInterface {

    private final RestTemplate restTemplate;
    private final String internalApiKey;

    public RealRiskClient(RestTemplate restTemplate,
                          @org.springframework.beans.factory.annotation.Value("${internal.api.key}") String internalApiKey) {
        this.restTemplate = restTemplate;
        this.internalApiKey = internalApiKey;
    }

    @Override
    public RiskResult calculateRisk(Object riskPayload) {
        log.info("Calling RealRiskClient for risk service");

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Api-Key", internalApiKey);
        headers.set("Content-Type", "application/json");

        HttpEntity<Object> entity = new HttpEntity<>(riskPayload, headers);

        try {
            RiskResult result = restTemplate.postForObject(
                "http://risk-service/api/internal/risk/calculate",
                entity,
                RiskResult.class
            );
            log.info("Risk result received: rating={}", result.getRiskRating());
            return result;
        } catch (Exception e) {
            log.error("Failed to call risk service: {}", e.getMessage());
            throw new RuntimeException("Risk service call failed: " + e.getMessage(), e);
        }
    }
}
