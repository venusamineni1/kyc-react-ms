package com.venus.kyc.screening.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String API_KEY_SCHEME = "InternalApiKey";

    @Bean
    public OpenAPI screeningServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Screening Service API")
                        .description("""
                                NRTS-backed API for client sanctions and PEP screening.

                                **Authentication:** All `/api/internal/**` endpoints require the
                                `X-Internal-Api-Key` header. Click **Authorize** and enter the key.

                                **Default dev key:** `dev-internal-kyc-key-change-in-prod`
                                """)
                        .version("2.0"))
                // Register the API key security scheme
                .components(new Components()
                        .addSecuritySchemes(API_KEY_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-Internal-Api-Key")
                                .description("Internal service-to-service API key")))
                // Apply it globally to all endpoints
                .addSecurityItem(new SecurityRequirement().addList(API_KEY_SCHEME));
    }
}
