package com.venus.kyc.orchestration.client;

import com.venus.kyc.orchestration.dto.KycPrecheckRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ViewerClient {

    private final RestTemplate restTemplate;
    private static final String VIEWER_URL = "http://VIEWER/api/prospects/onboard";

    public String onboardUser(KycPrecheckRequest request) {
        log.info("Calling ViewerService at {}", VIEWER_URL);
        try {
            // Mocking RestTemplate call to onboard user. In reality:
            // return restTemplate.postForObject(VIEWER_URL, request, String.class);
            return UUID.randomUUID().toString();
        } catch (Exception e) {
            log.error("ViewerService call failed: {}", e.getMessage());
            throw e;
        }
    }
}
