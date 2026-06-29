package com.venus.kyc.orchestration.client;

import com.venus.kyc.orchestration.dto.KycPrecheckRequest;
import lombok.Data;

import java.util.List;

/**
 * Abstraction for screening service client. Allows switching between mock and real implementations.
 */
public interface ScreeningClientInterface {

    /**
     * @param transactionId the KycTransactionAudit row id for this precheck — real implementations
     *                       use it to link the recorded KycOrchestrationEvent back to the transaction
     */
    ScreeningResult initiateScreening(KycPrecheckRequest request, Long transactionId);

    @Data
    class ScreeningResult {
        private String screeningRequestId;
        private String hit;
        private List<String> hitContext;
    }
}
