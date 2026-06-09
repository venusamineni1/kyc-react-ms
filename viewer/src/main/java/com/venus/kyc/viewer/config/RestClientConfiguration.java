package com.venus.kyc.viewer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Configuration for RestClient bean used for inter-service communication.
 * Makes RestClient available for dependency injection across the application.
 */
@Configuration
public class RestClientConfiguration {

    @Bean
    public RestClient restClient() {
        return RestClient.create();
    }
}
