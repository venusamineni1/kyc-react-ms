package com.venus.kyc.orchestration.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@Schema(description = "Result of a KYC pre-check initiation. Contains the orchestration outcome, " +
                      "screening result, risk rating, and latency telemetry for the downstream calls.")
public class KycPrecheckResponse {

    // -------------------------------------------------------------------------
    // Orchestration state
    // -------------------------------------------------------------------------

    @Schema(description = "Opaque, non-sequential KYC identifier. Retain this value to query " +
                          "GET /api/v1/kyc/{kycId}/status or match incoming webhook notifications. " +
                          "The identifier encodes the database row id via Hashids and reveals no record volume.",
            example = "nR7kW2pL")
    private String kycId;

    @Schema(description = "Orchestration outcome. 'APPROVED' means onboarding can proceed immediately. " +
                          "'ON_HOLD' means a screening hit or HIGH risk was detected and KYC analyst review is required.",
            example = "ON_HOLD",
            allowableValues = {"APPROVED", "ON_HOLD"})
    private String kycStatus;

    // -------------------------------------------------------------------------
    // Screening outcome
    // -------------------------------------------------------------------------

    @Schema(description = "Raw screening result returned by NLS/NRTS.",
            example = "Hit",
            allowableValues = {"Hit", "NoHit"})
    private String screeningResult;

    @Schema(description = "Context types associated with a screening Hit (e.g. PEP, SAN, ADM). " +
                          "Empty list when screeningResult is 'NoHit'.",
            example = "[\"PEP\"]")
    private List<String> hitContext;

    @Schema(description = "CRRE risk rating.",
            example = "HIGH",
            allowableValues = {"LOW", "MEDIUM", "HIGH", "UNKNOWN"})
    private String riskRating;

    // -------------------------------------------------------------------------
    // Traceability IDs
    // -------------------------------------------------------------------------

    @Schema(description = "User ID assigned by KYC-NCA ingestion. May be null if NCA ingestion failed (non-fatal).",
            example = "usr-7f3a1c2d")
    private String userId;

    @Schema(description = "Traceability ID for the NLS/NRTS screening call.",
            example = "scr-a1b2c3d4-e5f6")
    private String screeningRequestId;

    @Schema(description = "Traceability ID for the CRRE risk rating call.",
            example = "rsk-f6e5d4c3-b2a1")
    private String riskRequestId;

    // -------------------------------------------------------------------------
    // Latency telemetry
    // -------------------------------------------------------------------------

    @Schema(description = "Timestamp when NLS/NRTS screening was initiated (ISO-8601).",
            example = "2026-04-11T10:01:00.100")
    private LocalDateTime screeningStartAt;

    @Schema(description = "Timestamp when NLS/NRTS screening result was received (ISO-8601).",
            example = "2026-04-11T10:01:00.380")
    private LocalDateTime screeningEndAt;

    @Schema(description = "Timestamp when CRRE risk rating was initiated (ISO-8601).",
            example = "2026-04-11T10:01:00.381")
    private LocalDateTime riskStartAt;

    @Schema(description = "Timestamp when CRRE risk rating result was received (ISO-8601).",
            example = "2026-04-11T10:01:00.650")
    private LocalDateTime riskEndAt;
}
