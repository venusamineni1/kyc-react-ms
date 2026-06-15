package com.venus.kyc.orchestration.client;

import lombok.Data;

/**
 * Abstraction for risk service client. Allows switching between mock and real implementations.
 */
public interface RiskClientInterface {

    RiskResult calculateRisk(Object riskPayload);

    @Data
    class RiskResult {
        private String riskRequestId;
        private String riskRating;
        private Integer riskScore;
    }
}
