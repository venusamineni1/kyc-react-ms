package com.venus.kyc.orchestration.domain.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Lifecycle status of a KYC record.",
        enumAsRef = true)
public enum KycStatus {

    @Schema(description = "Initial state: audit record persisted, downstream calls (NLS, CRRE, NCA) still in flight. " +
                          "Transient — should resolve within seconds.")
    PENDING,

    @Schema(description = "No screening hit and risk rating is not HIGH. Onboarding can proceed immediately.")
    APPROVED,

    @Schema(description = "Screening returned a Hit OR risk rating is HIGH. " +
                          "Onboarding is suspended pending KYC analyst review in NCA.")
    ON_HOLD,

    @Schema(description = "KYC-NCA analyst review is complete and a final decision has been communicated.")
    COMPLETED
}
