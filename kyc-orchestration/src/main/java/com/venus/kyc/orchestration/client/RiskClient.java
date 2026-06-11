package com.venus.kyc.orchestration.client;

/**
 * @deprecated Use {@link RiskClientInterface} with {@link MockRiskClient} or {@link RealRiskClient} instead.
 * This class has been split into two implementations with @ConditionalOnProperty to support
 * switching between mock and real HTTP calls based on configuration.
 */
@Deprecated(forRemoval = true)
public class RiskClient {
    // This file is kept for reference only. Use RiskClientInterface instead.
}
