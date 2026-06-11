package com.venus.kyc.orchestration.client;

import com.venus.kyc.orchestration.dto.KycPrecheckRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@ConditionalOnProperty(name = "orchestration.screening.mock-enabled", havingValue = "false")
@Slf4j
public class RealScreeningClient implements ScreeningClientInterface {

    private final RestTemplate restTemplate;
    private final String internalApiKey;

    public RealScreeningClient(RestTemplate restTemplate,
                               @org.springframework.beans.factory.annotation.Value("${internal.api.key}") String internalApiKey) {
        this.restTemplate = restTemplate;
        this.internalApiKey = internalApiKey;
    }

    @Override
    public ScreeningResult initiateScreening(KycPrecheckRequest request) {
        log.info("Calling RealScreeningClient for screening service");

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Api-Key", internalApiKey);
        headers.set("Content-Type", "application/json");

        HttpEntity<KycPrecheckRequest> entity = new HttpEntity<>(request, headers);

        try {
            ScreeningResult result = restTemplate.postForObject(
                "http://screening-service/api/internal/screening/initiate",
                entity,
                ScreeningResult.class
            );
            log.info("Screening result received: hit={}", result.getHit());
            return result;
        } catch (Exception e) {
            log.error("Failed to call screening service: {}", e.getMessage());
            throw new RuntimeException("Screening service call failed: " + e.getMessage(), e);
        }
    }
}
