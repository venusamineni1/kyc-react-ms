package com.venus.kyc.risk.crre;

import com.venus.kyc.risk.RiskDTOs;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;

/**
 * HTTP client for the real CRRE API.
 * Supports two modes:
 *   - mtls.enabled=false : plain HTTPS (for non-prod CRRE environments)
 *   - mtls.enabled=true  : mutual TLS using a client keystore + optional truststore
 *
 * Active only when crre.mock=false.
 */
@Component
@ConditionalOnProperty(name = "crre.mock", havingValue = "false")
public class CrreHttpClient {

    private static final Logger log = LoggerFactory.getLogger(CrreHttpClient.class);

    private final RestClient restClient;

    public CrreHttpClient(CrreConfig config) throws Exception {
        log.info("Initialising CRRE HTTP client — baseUrl={}, mTLS={}",
                config.baseUrl(), config.mtls() != null && config.mtls().enabled());

        HttpComponentsClientHttpRequestFactory factory = (config.mtls() != null && config.mtls().enabled())
                ? buildMtlsFactory(config)
                : buildPlainFactory(config);

        this.restClient = RestClient.builder()
                .baseUrl(config.baseUrl())
                .requestFactory(factory)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Accept",       MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Posts a risk calculation request to the CRRE API.
     * The CRRE endpoint is POST / (root path — baseUrl is the full endpoint URL).
     */
    public RiskDTOs.CalculateRiskResponse calculateRisk(RiskDTOs.CalculateRiskRequest request) {
        log.debug("Calling CRRE API for recordId={}",
                request.clientRiskRatingRequest() != null && !request.clientRiskRatingRequest().isEmpty()
                        ? request.clientRiskRatingRequest().get(0).clientDetails().recordID()
                        : "unknown");

        return restClient.post()
                .uri("") // baseUrl IS the full CRRE endpoint
                .body(request)
                .retrieve()
                .onStatus(status -> status.isError(), (req, res) -> {
                    throw new CrreApiException(res.getStatusCode().value(),
                            "CRRE API error: HTTP " + res.getStatusCode().value());
                })
                .body(RiskDTOs.CalculateRiskResponse.class);
    }

    // ── mTLS factory ──────────────────────────────────────────────────────────

    private HttpComponentsClientHttpRequestFactory buildMtlsFactory(CrreConfig config) throws Exception {
        CrreConfig.Mtls mtls = config.mtls();
        log.info("Building mTLS factory — keyStore={}, trustStore={}",
                mtls.keyStorePath(),
                mtls.trustStorePath() != null && !mtls.trustStorePath().isBlank()
                        ? mtls.trustStorePath() : "<JVM default>");

        // 1. Client KeyManagers — our certificate + private key
        KeyStore keyStore = loadKeyStore(
                mtls.keyStorePath(), mtls.keyStorePassword(), mtls.keyStoreType());
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(
                KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, mtls.keyStorePassword().toCharArray());

        // 2. TrustManagers — CRRE server's CA cert
        //    Blank trustStorePath → use JVM default cacerts
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
        if (mtls.trustStorePath() != null && !mtls.trustStorePath().isBlank()) {
            KeyStore trustStore = loadKeyStore(
                    mtls.trustStorePath(), mtls.trustStorePassword(), mtls.trustStoreType());
            tmf.init(trustStore);
        } else {
            tmf.init((KeyStore) null); // JVM default truststore
        }

        // 3. SSLContext wiring both together
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        // 4. Apache HttpClient 5 with mTLS SSLContext
        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(
                        PoolingHttpClientConnectionManagerBuilder.create()
                                .setSSLSocketFactory(
                                        SSLConnectionSocketFactoryBuilder.create()
                                                .setSslContext(sslContext)
                                                .build())
                                .setDefaultSocketConfig(
                                        SocketConfig.custom()
                                                .setSoTimeout(
                                                        Timeout.ofMilliseconds(config.httpTimeoutMs()))
                                                .build())
                                .build())
                .build();

        return new HttpComponentsClientHttpRequestFactory(httpClient);
    }

    // ── Plain HTTPS factory (mTLS disabled) ───────────────────────────────────

    private HttpComponentsClientHttpRequestFactory buildPlainFactory(CrreConfig config) {
        log.info("Building plain HTTPS factory (mTLS disabled)");
        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(
                        PoolingHttpClientConnectionManagerBuilder.create()
                                .setDefaultSocketConfig(
                                        SocketConfig.custom()
                                                .setSoTimeout(
                                                        Timeout.ofMilliseconds(config.httpTimeoutMs()))
                                                .build())
                                .build())
                .build();
        return new HttpComponentsClientHttpRequestFactory(httpClient);
    }

    // ── Keystore loader ───────────────────────────────────────────────────────

    private KeyStore loadKeyStore(String path, String password, String type) throws Exception {
        KeyStore ks = KeyStore.getInstance(type);
        try (InputStream is = new FileInputStream(path)) {
            ks.load(is, password != null ? password.toCharArray() : null);
        }
        return ks;
    }

    // ── Exception ─────────────────────────────────────────────────────────────

    public static class CrreApiException extends RuntimeException {
        private final int httpStatus;
        public CrreApiException(int httpStatus, String message) {
            super(message);
            this.httpStatus = httpStatus;
        }
        public int getHttpStatus() { return httpStatus; }
    }
}
