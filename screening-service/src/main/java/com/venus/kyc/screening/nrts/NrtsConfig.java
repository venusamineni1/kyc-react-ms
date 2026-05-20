package com.venus.kyc.screening.nrts;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "nrts")
public record NrtsConfig(
        String baseUrl,
        int srcId,
        String username,
        String password,
        @DefaultValue("0") long statusCheckDelayMs,
        @DefaultValue("30000") long httpTimeoutMs,
        @DefaultValue("true") boolean mock
) {}
