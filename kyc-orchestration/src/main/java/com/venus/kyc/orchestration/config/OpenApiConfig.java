package com.venus.kyc.orchestration.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configures the OpenAPI / Swagger specification for the KYC Orchestration Service.
 * The Swagger UI is available at: http://localhost:8086/swagger-ui/index.html
 * The raw OpenAPI JSON is available at: http://localhost:8086/v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI kycOrchestrationOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("KYC Orchestration Service")
                        .version("1.0.0")
                        .description("""
                                Orchestrates KYC (Know Your Customer) pre-check flows for **EIS** and **PPR** business lines.

                                The service coordinates three downstream systems in a single synchronous call:
                                - **KYC-NCA** — data ingestion and prospect onboarding
                                - **NLS/NRTS** — real-time sanctions and PEP screening
                                - **CRRE** — risk rating engine

                                **On-hold behaviour:** If screening returns a Hit or the risk rating is HIGH, the record is \
                                placed `ON_HOLD`. The caller retains the `kycId` and either polls \
                                `GET /api/v1/kyc/{kycId}/status` or awaits a webhook notification when \
                                the status changes.

                                **PII protection:** `firstName`, `lastName`, `dob`, `residentialAddress.addressLine1`, \
                                `residentialAddress.addressLine2`, `identificationNumber`, and `germanTaxID` are stored \
                                AES-128/GCM encrypted at rest with a per-record random IV.

                                **kycId:** An opaque, non-sequential string that encodes the database row id via \
                                Hashids. It reveals no record volume and cannot be guessed or enumerated.
                                """)
                        .contact(new Contact()
                                .name("KYC Platform Team")
                                .email("kyc-platform@internal.example.com")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8086")
                                .description("Local development"),
                        new Server()
                                .url("http://kyc-orchestration")
                                .description("Eureka service reference (internal)")))
                .tags(List.of(
                        new Tag()
                                .name("KYC Precheck")
                                .description("Initiate a KYC pre-check for a new client"),
                        new Tag()
                                .name("KYC Status")
                                .description("Query the current status of an existing KYC record"),
                        new Tag()
                                .name("Internal Callbacks")
                                .description("Internal endpoint used by KYC-NCA to push status updates — " +
                                             "**not** part of the EIS/PPR Gateway integration")));
    }
}
