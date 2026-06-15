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
            // Call screening-service and get its response format
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> serviceResponse = restTemplate.postForObject(
                "http://screening-service/api/internal/screening/initiate",
                entity,
                java.util.Map.class
            );

            // Transform to orchestration format
            ScreeningResult result = new ScreeningResult();

            // Map result field: "Hot" → "Hit", "No-Hit" → "NoHit"
            String serviceResult = (String) serviceResponse.get("result");
            result.setHit("Hot".equals(serviceResult) ? "Hit" : "NoHit");

            // Map alert contexts to hit context
            @SuppressWarnings("unchecked")
            java.util.List<String> alertContexts = (java.util.List<String>) serviceResponse.get("alertContexts");
            result.setHitContext(alertContexts != null ? alertContexts : java.util.List.of());

            // Use processId as the request ID
            Object processId = serviceResponse.get("processId");
            result.setScreeningRequestId(processId != null ? processId.toString() : "no-hit-" + System.currentTimeMillis());

            log.info("Screening result transformed: hit={}, contexts={}", result.getHit(), result.getHitContext());
            return result;
        } catch (Exception e) {
            log.error("Failed to call screening service: {}", e.getMessage());
            throw new RuntimeException("Screening service call failed: " + e.getMessage(), e);
        }
    }
}
