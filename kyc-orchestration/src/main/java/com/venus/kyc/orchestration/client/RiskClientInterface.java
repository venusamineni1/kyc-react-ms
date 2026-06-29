package com.venus.kyc.orchestration.client;

import lombok.Data;

/**
 * Abstraction for risk service client. Allows switching between mock and real implementations.
 */
public interface RiskClientInterface {

    /**
     * @param transactionId the KycTransactionAudit row id for this precheck — real implementations
     *                       use it to link the recorded KycOrchestrationEvent back to the transaction
     */
    RiskResult calculateRisk(Object riskPayload, Long transactionId);

    @Data
    class RiskResult {
        private String riskRequestId;
        private String riskRating;
        private Integer riskScore;
    }
}
