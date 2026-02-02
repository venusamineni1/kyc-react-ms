package com.venus.kyc.viewer.risk;

import java.time.LocalDateTime;

public record RiskAssessment(
        Long assessmentID,
        Long logID,
        String recordID,
        Integer overallRiskScore,
        String initialRiskLevel,
        String overallRiskLevel,
        String typeOfLogicApplied,
        String smeRiskAssessment,
        LocalDateTime createdAt) {
}
