package com.venus.kyc.orchestration.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    /**
     * Load-balanced RestTemplate for calls to Eureka-registered services
     * (ScreeningClient, RiskClient, ViewerClient).
     * Connect/read timeouts are set here so a stalled downstream service fails fast
     * instead of hanging the calling thread indefinitely; Resilience4j circuit breakers
     * on the clients then stop hammering a service that keeps timing out.
     */
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate(
            @Value("${orchestration.http.connect-timeout-ms:3000}") long connectTimeoutMs,
            @Value("${orchestration.http.read-timeout-ms:5000}") long readTimeoutMs) {
        return new RestTemplate(clientHttpRequestFactory(connectTimeoutMs, readTimeoutMs));
    }

    /**
     * Plain RestTemplate for outbound calls to external, non-Eureka URLs
     * such as EIS/PPR Gateway webhook endpoints.
     * Must NOT be @LoadBalanced — that annotation requires Eureka service IDs, not real URLs.
     */
    @Bean
    @Qualifier("externalRestTemplate")
    public RestTemplate externalRestTemplate(
            @Value("${orchestration.http.connect-timeout-ms:3000}") long connectTimeoutMs,
            @Value("${orchestration.http.read-timeout-ms:5000}") long readTimeoutMs) {
        return new RestTemplate(clientHttpRequestFactory(connectTimeoutMs, readTimeoutMs));
    }

    private ClientHttpRequestFactory clientHttpRequestFactory(long connectTimeoutMs, long readTimeoutMs) {
        return ClientHttpRequestFactoryBuilder.detect().build(ClientHttpRequestFactorySettings.defaults()
                .withConnectTimeout(Duration.ofMillis(connectTimeoutMs))
                .withReadTimeout(Duration.ofMillis(readTimeoutMs)));
    }
}
