package com.venus.kyc.orchestration.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
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
    @CircuitBreaker(name = "riskService", fallbackMethod = "calculateRiskFallback")
    public RiskResult calculateRisk(Object riskPayload) {
        log.info("Calling RealRiskClient for risk service");

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Api-Key", internalApiKey);
        headers.set("Content-Type", "application/json");

        HttpEntity<Object> entity = new HttpEntity<>(riskPayload, headers);

        try {
            // Call risk-service and get its response
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> serviceResponse = restTemplate.postForObject(
                "http://risk-service/api/v1/internal/risk/calculate",
                entity,
                java.util.Map.class
            );

            // Transform to orchestration format
            RiskResult result = new RiskResult();

            // Extract risk rating from response
            // Service returns complex structure; extract the rating if present
            String riskRating = extractRiskRating(serviceResponse);
            result.setRiskRating(riskRating != null ? riskRating : "LOW");
            result.setRiskScore(extractRiskScore(serviceResponse));

            // Use a generated request ID
            result.setRiskRequestId(java.util.UUID.randomUUID().toString());

            log.info("Risk result transformed: rating={} score={}", result.getRiskRating(), result.getRiskScore());
            return result;
        } catch (Exception e) {
            log.error("Failed to call risk service: {}", e.getMessage());
            throw new RuntimeException("Risk service call failed: " + e.getMessage(), e);
        }
    }

    private String extractRiskRating(java.util.Map<String, Object> response) {
        java.util.Map<String, Object> overallRiskAssessment = extractOverallRiskAssessment(response);
        if (overallRiskAssessment != null) {
            Object overallLevel = overallRiskAssessment.get("overallRiskLevel");
            if (overallLevel != null) {
                return overallLevel.toString().toUpperCase();
            }
        }

        if (response == null) {
            return null;
        }

        // Fallback: look for top-level riskRating/riskLevel fields
        Object ratingObj = response.get("riskRating");
        if (ratingObj != null) {
            return ratingObj.toString().toUpperCase();
        }

        Object levelObj = response.get("riskLevel");
        if (levelObj != null) {
            return levelObj.toString().toUpperCase();
        }

        return null;
    }

    private Integer extractRiskScore(java.util.Map<String, Object> response) {
        java.util.Map<String, Object> overallRiskAssessment = extractOverallRiskAssessment(response);
        if (overallRiskAssessment != null) {
            Object scoreObj = overallRiskAssessment.get("overallRiskScore");
            if (scoreObj instanceof Number number) {
                return number.intValue();
            }
        }

        if (response != null) {
            Object scoreObj = response.get("overallRiskScore");
            if (scoreObj instanceof Number number) {
                return number.intValue();
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private java.util.Map<String, Object> extractOverallRiskAssessment(java.util.Map<String, Object> response) {
        if (response == null) {
            return null;
        }

        Object clientRiskRatingResponse = response.get("clientRiskRatingResponse");
        if (clientRiskRatingResponse instanceof java.util.List<?> list && !list.isEmpty()
                && list.get(0) instanceof java.util.Map<?, ?> firstItem) {
            Object overallRiskAssessment = firstItem.get("overallRiskAssessment");
            if (overallRiskAssessment instanceof java.util.Map<?, ?> overall) {
                return (java.util.Map<String, Object>) overall;
            }
        }

        return null;
    }

    /**
     * Invoked when the risk-service call times out, errors repeatedly, or the circuit is open.
     * Risk rating must never be guessed, so this fails loudly rather than returning a default rating.
     */
    private RiskResult calculateRiskFallback(Object riskPayload, Throwable t) {
        log.error("Risk service unavailable (circuit breaker): {}", t.getMessage());
        throw new RuntimeException("Risk service unavailable: " + t.getMessage(), t);
    }
}
