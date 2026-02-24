package com.venus.kyc.risk.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI riskServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("Risk Service API")
                        .description("API for Risk Assessment and Management")
                        .version("1.0"));
    }
}
