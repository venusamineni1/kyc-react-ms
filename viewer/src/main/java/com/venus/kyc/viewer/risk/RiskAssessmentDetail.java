package com.venus.kyc.viewer.risk;

public record RiskAssessmentDetail(
        Long detailID,
        Long assessmentID,
        String riskType,
        String elementName,
        String elementValue,
        Integer riskScore,
        String flag,
        String localRuleApplied) {
}
