package com.venus.kyc.orchestration.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.venus.kyc.orchestration.domain.KycOrchestrationEvent;
import com.venus.kyc.orchestration.dto.KycPrecheckRequest;
import com.venus.kyc.orchestration.repository.KycOrchestrationEventRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
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
    private final KycOrchestrationEventRepository eventRepository;
    private final ObjectMapper objectMapper;

    public RealScreeningClient(RestTemplate restTemplate,
                               @org.springframework.beans.factory.annotation.Value("${internal.api.key}") String internalApiKey,
                               KycOrchestrationEventRepository eventRepository,
                               ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.internalApiKey = internalApiKey;
        this.eventRepository = eventRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @CircuitBreaker(name = "screeningService", fallbackMethod = "initiateScreeningFallback")
    public ScreeningResult initiateScreening(KycPrecheckRequest request, Long transactionId) {
        log.info("Calling RealScreeningClient for screening service");

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Api-Key", internalApiKey);
        headers.set("Content-Type", "application/json");

        HttpEntity<KycPrecheckRequest> entity = new HttpEntity<>(request, headers);

        try {
            // Call screening-service and get its response format
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> serviceResponse = restTemplate.postForObject(
                "http://screening-service/api/v1/internal/screening/initiate",
                entity,
                java.util.Map.class
            );

            // Record the full raw response as an append-only event before extracting just the
            // fields below — this is the data that used to be discarded once parsed.
            recordEvent(transactionId, request.getUniqueClientID(), serviceResponse);

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

    private void recordEvent(Long transactionId, String uniqueClientId, java.util.Map<String, Object> serviceResponse) {
        try {
            KycOrchestrationEvent event = new KycOrchestrationEvent();
            event.setKycTransactionId(transactionId);
            event.setUniqueClientID(uniqueClientId);
            event.setEventType("SCREENING_RESULT");
            event.setSource("screening-service");
            event.setDownstreamResponse(objectMapper.writeValueAsString(serviceResponse));
            eventRepository.save(event);
        } catch (Exception e) {
            // Never let audit-trail persistence break the actual orchestration flow.
            log.error("Failed to record SCREENING_RESULT event for transactionId={}: {}", transactionId, e.getMessage(), e);
        }
    }

    /**
     * Invoked when the screening-service call times out, errors repeatedly, or the circuit is open.
     * A hit/no-hit result must never be guessed, so this fails loudly rather than assuming "NoHit".
     */
    private ScreeningResult initiateScreeningFallback(KycPrecheckRequest request, Long transactionId, Throwable t) {
        log.error("Screening service unavailable (circuit breaker): {}", t.getMessage());
        throw new RuntimeException("Screening service unavailable: " + t.getMessage(), t);
    }
}
