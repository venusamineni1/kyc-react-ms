package com.venus.kyc.orchestration.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Nested DTO representing a client's residential address.
 * addressLine1, city, and zip are mandatory; addressLine2 is optional.
 * addressLine1 and addressLine2 are stored AES-GCM encrypted in the database.
 */
@Data
@Schema(description = "Client's current residential address")
public class ResidentialAddress {

    @Schema(description = "Street address line 1. Stored AES-GCM encrypted at rest.",
            example = "Unter den Linden 1",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "residentialAddress.addressLine1 is required")
    private String addressLine1;

    @Schema(description = "Street address line 2 (apartment, floor, etc.). Stored AES-GCM encrypted at rest when present.",
            example = "Apt 4B",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String addressLine2;

    @Schema(description = "City.",
            example = "Berlin",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "residentialAddress.city is required")
    private String city;

    @Schema(description = "Postal / ZIP code.",
            example = "10117",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "residentialAddress.zip is required")
    private String zip;
}
