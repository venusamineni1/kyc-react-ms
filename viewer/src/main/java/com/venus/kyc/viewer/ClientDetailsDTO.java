package com.venus.kyc.viewer;

import com.venus.kyc.viewer.screening.ScreeningLog;
import com.venus.kyc.viewer.screening.ScreeningResult;
import com.venus.kyc.viewer.risk.RiskAssessment;
import com.venus.kyc.viewer.risk.RiskAssessmentDetail;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO that combines Client details with the latest screening and risk results.
 * Used to display comprehensive client profile information including KYC precheck outcomes.
 */
public record ClientDetailsDTO(
        Client client,
        ScreeningLog latestScreening,
        List<ScreeningResult> screeningResults,
        RiskAssessment latestRiskAssessment,
        List<RiskAssessmentDetail> riskAssessmentDetails,
        String lastCheckedAt) {

    public ClientDetailsDTO(Client client, ScreeningLog screening, List<ScreeningResult> srResults,
                          RiskAssessment risk, List<RiskAssessmentDetail> raDetails) {
        this(client, screening, srResults, risk, raDetails, screening != null ? screening.createdAt().toString() : null);
    }
}
