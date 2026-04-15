package com.venus.kyc.orchestration.dto;

import com.venus.kyc.orchestration.domain.enums.KycStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Request body for the internal KYC-NCA status-update callback. " +
                      "Only APPROVED and COMPLETED are valid target statuses.")
public class KycStatusUpdateRequest {

    @Schema(description = "Target KYC status. Valid transitions: ON_HOLD → APPROVED or ON_HOLD → COMPLETED. " +
                          "PENDING and ON_HOLD are rejected (400). Updating a COMPLETED record is rejected (409).",
            example = "COMPLETED",
            allowableValues = {"APPROVED", "COMPLETED"},
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "kycStatus is required")
    private KycStatus kycStatus;
}
