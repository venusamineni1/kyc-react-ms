package com.venus.kyc.viewer.risk;

import java.time.LocalDateTime;

public record RiskAssessmentLog(
        Long logID,
        String requestJSON,
        String responseJSON,
        String status,
        LocalDateTime createdAt) {
}
