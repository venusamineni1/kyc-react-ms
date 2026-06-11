package com.venus.kyc.viewer;

import com.venus.kyc.viewer.screening.ScreeningLog;
import com.venus.kyc.viewer.screening.ScreeningResult;
import com.venus.kyc.viewer.risk.RiskAssessment;
import com.venus.kyc.viewer.risk.RiskAssessmentDetail;
import java.util.List;

/**
 * DTO that combines Case details with the latest screening and risk results from the associated client.
 * Used to display comprehensive case information with KYC precheck outcomes.
 */
public record CaseDetailsDTO(
        Case caseData,
        ScreeningLog latestScreening,
        List<ScreeningResult> screeningResults,
        RiskAssessment latestRiskAssessment,
        List<RiskAssessmentDetail> riskAssessmentDetails) {
}
