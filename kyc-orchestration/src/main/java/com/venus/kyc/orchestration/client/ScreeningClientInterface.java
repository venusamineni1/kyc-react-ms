package com.venus.kyc.orchestration.client;

import com.venus.kyc.orchestration.dto.KycPrecheckRequest;
import lombok.Data;

import java.util.List;

/**
 * Abstraction for screening service client. Allows switching between mock and real implementations.
 */
public interface ScreeningClientInterface {

    ScreeningResult initiateScreening(KycPrecheckRequest request);

    @Data
    class ScreeningResult {
        private String screeningRequestId;
        private String hit;
        private List<String> hitContext;
    }
}
