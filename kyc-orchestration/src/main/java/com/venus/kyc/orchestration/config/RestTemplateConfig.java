package com.venus.kyc.orchestration.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    /**
     * Load-balanced RestTemplate for calls to Eureka-registered services
     * (ScreeningClient, RiskClient, ViewerClient).
     */
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * Plain RestTemplate for outbound calls to external, non-Eureka URLs
     * such as EIS/PPR Gateway webhook endpoints.
     * Must NOT be @LoadBalanced — that annotation requires Eureka service IDs, not real URLs.
     */
    @Bean
    @Qualifier("externalRestTemplate")
    public RestTemplate externalRestTemplate() {
        return new RestTemplate();
    }
}
