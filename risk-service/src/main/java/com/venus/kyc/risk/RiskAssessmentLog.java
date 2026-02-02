package com.venus.kyc.risk;

import java.time.LocalDateTime;

public record RiskAssessmentLog(
                Long logID,
                String requestJSON,
                String responseJSON,
                String status,
                LocalDateTime createdAt) {
}
