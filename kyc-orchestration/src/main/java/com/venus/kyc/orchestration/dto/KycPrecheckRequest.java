package com.venus.kyc.orchestration.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "Request body for initiating a KYC pre-check on a new client")
public class KycPrecheckRequest {

    // -------------------------------------------------------------------------
    // Core identity
    // -------------------------------------------------------------------------

    @Schema(description = "Unique client identifier supplied by the EIS/PPR Gateway. " +
                          "Used as the primary correlation key across all downstream calls.",
            example = "CLIENT-20260411-00123",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "uniqueClientID is required")
    private String uniqueClientID;

    @Schema(description = "Client's first name. Stored AES-GCM encrypted at rest.",
            example = "Jane",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "firstName is required")
    private String firstName;

    @Schema(description = "Client's middle name (optional).",
            example = "Marie",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String middleName;

    @Schema(description = "Client's last name. Stored AES-GCM encrypted at rest.",
            example = "Smith",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "lastName is required")
    private String lastName;

    @Schema(description = "Client's title.",
            example = "Ms",
            allowableValues = {"Mr", "Mrs", "Ms", "Miss", "Dr", "Prof"},
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String title;

    @Schema(description = "Client's date of birth in YYYY-MM-DD format. Stored AES-GCM encrypted at rest.",
            example = "1985-06-15",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "dob is required")
    private String dob;

    @Schema(description = "Originating business line. Must be exactly 'EIS' or 'PPR'.",
            example = "EIS",
            allowableValues = {"EIS", "PPR"},
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "businessLine is required")
    @Pattern(regexp = "EIS|PPR", message = "businessLine must be EIS or PPR")
    private String businessLine;

    // -------------------------------------------------------------------------
    // Biographical / citizenship
    // -------------------------------------------------------------------------

    @Schema(description = "City where the client was born.",
            example = "Hamburg",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "cityOfBirth is required")
    private String cityOfBirth;

    @Schema(description = "Country of birth as an ISO 3166-1 alpha-2 code.",
            example = "DE",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String countryOfBirth;

    @Schema(description = "Primary citizenship country as an ISO 3166-1 alpha-2 code.",
            example = "DE",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "primaryCitizenship is required")
    private String primaryCitizenship;

    @Schema(description = "Second citizenship country code, if applicable.",
            example = "US",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String secondCitizenship;

    // -------------------------------------------------------------------------
    // Residential address
    // -------------------------------------------------------------------------

    @Schema(description = "Client's current residential address. addressLine1, city, and zip are mandatory.",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    @NotNull(message = "residentialAddress is required")
    private ResidentialAddress residentialAddress;

    @Schema(description = "Country of residence as an ISO 3166-1 alpha-2 code.",
            example = "DE",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "countryOfResidence is required")
    private String countryOfResidence;

    // -------------------------------------------------------------------------
    // Occupation
    // -------------------------------------------------------------------------

    @Schema(description = "Client's occupation or job title.",
            example = "Software Engineer",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String occupation;

    // -------------------------------------------------------------------------
    // Legitimisation document
    // -------------------------------------------------------------------------

    @Schema(description = "Type of identity document used for legitimisation.",
            example = "PASSPORT",
            allowableValues = {"PASSPORT", "NATIONAL_ID", "DRIVING_LICENCE", "RESIDENCE_PERMIT"},
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String typeOfLegitimizationDocument;

    @Schema(description = "Name of the authority that issued the legitimisation document.",
            example = "Bundesdruckerei",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String issuingAuthority;

    @Schema(description = "Document identification number. Stored AES-GCM encrypted at rest.",
            example = "C01X00T47",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String identificationNumber;

    @Schema(description = "Document expiry date in YYYY-MM-DD format.",
            example = "2030-08-15",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String expirationDate;

    // -------------------------------------------------------------------------
    // Tax identifier
    // -------------------------------------------------------------------------

    @Schema(description = "German tax identification number (Steuer-IdNr). Stored AES-GCM encrypted at rest.",
            example = "86095742719",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String germanTaxID;

    // -------------------------------------------------------------------------
    // Webhook
    // -------------------------------------------------------------------------

    @Schema(description = "Optional callback URL. If supplied, KYC Orchestration will POST a KycStatusResponse " +
                          "to this URL when the status transitions out of ON_HOLD. " +
                          "Must be a valid absolute HTTPS URL in production environments.",
            example = "https://eis-gateway.internal/api/kyc/callbacks/status",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String webhookUrl;
}
