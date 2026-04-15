package com.venus.kyc.orchestration.domain.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Result of the NLS/NRTS real-time screening call.",
        enumAsRef = true)
public enum ScreeningStatus {

    @Schema(description = "NLS/NRTS matched one or more entries (sanctions, PEP, adverse media). " +
                          "Match types are listed in screeningContext.")
    HIT,

    @Schema(description = "NLS/NRTS found no matches for the client.")
    NO_HIT
}
