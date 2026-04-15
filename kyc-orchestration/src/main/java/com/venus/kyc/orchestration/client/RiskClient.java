package com.venus.kyc.orchestration.client;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class RiskClient {

    private final RestTemplate restTemplate;
    private static final String RISK_URL = "http://RISK-SERVICE/api/internal/risk/calculate";

    public RiskResult calculateRisk(Object riskPayload) {
        log.info("Calling RiskService at {}", RISK_URL);
        // Mocked response for scaffolding
        RiskResult result = new RiskResult();
        result.setRiskRequestId(UUID.randomUUID().toString());
        result.setRiskRating("LOW");
        return result;
    }

    @Data
    public static class RiskResult {
        private String riskRequestId;
        private String riskRating;
    }
}
