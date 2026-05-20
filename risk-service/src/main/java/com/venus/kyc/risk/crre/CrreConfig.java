package com.venus.kyc.risk.crre;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Typed configuration for the CRRE (Client Risk Rating Engine) external API.
 * Bound from application.properties prefix "crre".
 */
@ConfigurationProperties(prefix = "crre")
public record CrreConfig(
        String baseUrl,
        String callerSystem,
        String crrmVersion,
        @DefaultValue("30000") long httpTimeoutMs,
        @DefaultValue("true")  boolean mock,
        Mtls mtls
) {
    /**
     * mTLS configuration.
     * keyStorePath  — path to PKCS12/JKS keystore containing the client cert + private key.
     * trustStorePath — path to truststore containing the CRRE server's CA cert.
     *                  Leave blank to use the JVM default cacerts
     *                  (correct when CRRE uses a public CA like DigiCert).
     */
    public record Mtls(
            @DefaultValue("false") boolean enabled,
            String keyStorePath,
            String keyStorePassword,
            @DefaultValue("PKCS12") String keyStoreType,
            String trustStorePath,
            String trustStorePassword,
            @DefaultValue("PKCS12") String trustStoreType
    ) {}
}
