package com.venus.kyc.orchestration.domain.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "CRRE risk rating assigned to a client.",
        enumAsRef = true)
public enum RiskRating {

    @Schema(description = "Low risk — no additional action beyond standard onboarding process.")
    LOW,

    @Schema(description = "Medium risk — subject to enhanced due diligence per compliance policy.")
    MEDIUM,

    @Schema(description = "High risk — triggers ON_HOLD status and mandatory KYC analyst review.")
    HIGH,

    @Schema(description = "CRRE call failed or returned an unparseable result. " +
                          "Consumers should apply their own risk escalation policy for this value; " +
                          "the service does not automatically place the record ON_HOLD for UNKNOWN alone.")
    UNKNOWN
}
