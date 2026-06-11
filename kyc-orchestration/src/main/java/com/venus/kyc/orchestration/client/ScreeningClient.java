package com.venus.kyc.orchestration.client;

/**
 * @deprecated Use {@link ScreeningClientInterface} with {@link MockScreeningClient} or {@link RealScreeningClient} instead.
 * This class has been split into two implementations with @ConditionalOnProperty to support
 * switching between mock and real HTTP calls based on configuration.
 */
@Deprecated(forRemoval = true)
public class ScreeningClient {
    // This file is kept for reference only. Use ScreeningClientInterface instead.
}
