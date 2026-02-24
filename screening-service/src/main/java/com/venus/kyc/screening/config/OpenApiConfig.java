package com.venus.kyc.screening.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI screeningServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("Screening Service API")
                        .description("API for Client Screening and Matching")
                        .version("1.0"));
    }
}
