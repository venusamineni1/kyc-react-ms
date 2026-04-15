package com.venus.kyc.orchestration.dto;

import com.venus.kyc.orchestration.domain.enums.KycStatus;
import com.venus.kyc.orchestration.domain.enums.RiskRating;
import com.venus.kyc.orchestration.domain.enums.ScreeningStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@Schema(description = "Current state of a KYC record. Returned by GET /api/v1/kyc/{kycId}/status " +
                      "and as the webhook payload when a status transition occurs.")
public class KycStatusResponse {

    @Schema(description = "Opaque KYC identifier — the same value returned at initiation.",
            example = "nR7kW2pL")
    private String kycId;

    @Schema(description = "The client identifier originally supplied in the initiation request.",
            example = "CLIENT-20260411-00123")
    private String uniqueClientID;

    @Schema(description = "Originating business line.",
            example = "EIS",
            allowableValues = {"EIS", "PPR"})
    private String businessLine;

    @Schema(description = "Current KYC lifecycle status.")
    private KycStatus kycStatus;

    @Schema(description = "NLS/NRTS screening outcome. Null while the record is still PENDING.")
    private ScreeningStatus screeningStatus;

    @Schema(description = "Context types associated with a screening HIT (e.g. PEP, SAN, ADM). " +
                          "Empty list when screeningStatus is NO_HIT or record is PENDING.",
            example = "[\"PEP\"]")
    private List<String> screeningContext;

    @Schema(description = "CRRE risk rating. Null while the record is still PENDING. " +
                          "UNKNOWN indicates CRRE was unavailable or returned an unparseable result.")
    private RiskRating riskRating;

    @Schema(description = "NLS/NRTS traceability ID.",
            example = "scr-b2c3d4e5-f6a7")
    private String screeningRequestId;

    @Schema(description = "CRRE risk rating traceability ID.",
            example = "rsk-a7f6e5d4-c3b2")
    private String riskRequestId;

    @Schema(description = "Timestamp when the KYC record was first created (ISO-8601).",
            example = "2026-04-11T10:01:00.000")
    private LocalDateTime createdAt;

    @Schema(description = "Timestamp of the most recent status update (ISO-8601). Null on first read before any update.",
            example = "2026-04-11T14:32:00.000")
    private LocalDateTime updatedAt;
}
