package com.venus.kyc.orchestration.client;

import com.venus.kyc.orchestration.dto.KycPrecheckRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "orchestration.screening.mock-enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class MockScreeningClient implements ScreeningClientInterface {

    private final RestTemplate restTemplate;
    private static final String SCREENING_URL = "http://SCREENING-SERVICE/api/internal/screening/initiate";

    @Override
    public ScreeningResult initiateScreening(KycPrecheckRequest request, Long transactionId) {
        log.info("Calling MockScreeningClient (mocked response) for {}", SCREENING_URL);
        ScreeningResult result = new ScreeningResult();
        result.setScreeningRequestId(UUID.randomUUID().toString());
        result.setHit(Math.random() > 0.5 ? "Hit" : "NoHit");
        result.setHitContext(List.of("PEP"));
        return result;
    }
}
