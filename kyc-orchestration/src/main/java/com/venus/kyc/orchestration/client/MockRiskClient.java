package com.venus.kyc.orchestration.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Component
@ConditionalOnProperty(name = "orchestration.risk.mock-enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class MockRiskClient implements RiskClientInterface {

    private final RestTemplate restTemplate;
    private static final String RISK_URL = "http://RISK-SERVICE/api/internal/risk/calculate";

    @Override
    public RiskResult calculateRisk(Object riskPayload) {
        log.info("Calling MockRiskClient (mocked response) for {}", RISK_URL);
        RiskResult result = new RiskResult();
        result.setRiskRequestId(UUID.randomUUID().toString());
        result.setRiskRating("LOW");
        return result;
    }
}
