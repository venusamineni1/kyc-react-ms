package com.venus.kyc.risk;

/**
 * Abstraction over the external risk rating engine.
 *
 * Implementations:
 *   - MockRiskProvider   — crre.mock=true  (default) — pure Java, no network
 *   - CrreRiskProvider   — crre.mock=false — real CRRE API with mTLS
 */
public interface RiskProvider {

    /**
     * Calculates a client risk rating.
     *
     * @param request the full CRRE request (header + clientRiskRatingRequest list)
     * @return the full CRRE response (header + processStatus + clientRiskRatingResponse list)
     */
    RiskDTOs.CalculateRiskResponse calculateRisk(RiskDTOs.CalculateRiskRequest request);
}
