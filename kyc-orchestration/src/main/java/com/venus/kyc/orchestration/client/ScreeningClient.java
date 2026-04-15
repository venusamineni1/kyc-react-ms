package com.venus.kyc.orchestration.client;

import com.venus.kyc.orchestration.dto.KycPrecheckRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScreeningClient {

    private final RestTemplate restTemplate;
    private static final String SCREENING_URL = "http://SCREENING-SERVICE/api/internal/screening/initiate";

    public ScreeningResult initiateScreening(KycPrecheckRequest request) {
        log.info("Calling ScreeningService at {}", SCREENING_URL);
        // Mocked response for scaffolding
        ScreeningResult result = new ScreeningResult();
        result.setScreeningRequestId(UUID.randomUUID().toString());
        result.setHit(Math.random() > 0.5 ? "Hit" : "NoHit");
        result.setHitContext(List.of("PEP"));
        return result;
    }

    @Data
    public static class ScreeningResult {
        private String screeningRequestId;
        private String hit;
        private List<String> hitContext;
    }
}
