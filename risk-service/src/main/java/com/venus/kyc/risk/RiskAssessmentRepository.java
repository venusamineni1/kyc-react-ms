package com.venus.kyc.risk;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class RiskAssessmentRepository {

    private final JdbcClient jdbcClient;

    public RiskAssessmentRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public Long saveLog(RiskAssessmentLog log) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcClient.sql(
                "INSERT INTO RiskAssessmentLogs (RequestJSON, ResponseJSON, Status) VALUES (:requestJSON, :responseJSON, :status)")
                .param("requestJSON", log.requestJSON())
                .param("responseJSON", log.responseJSON())
                .param("status", log.status())
                .update(keyHolder);
        return ((Number) keyHolder.getKeys().get("LOGID")).longValue();
    }

    public Long saveAssessment(RiskAssessment assessment) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcClient.sql(
                "INSERT INTO RiskAssessments (LogID, RecordID, OverallRiskScore, InitialRiskLevel, OverallRiskLevel, TypeOfLogicApplied, SmeRiskAssessment) VALUES (:logID, :recordID, :overallRiskScore, :initialRiskLevel, :overallRiskLevel, :typeOfLogicApplied, :smeRiskAssessment)")
                .param("logID", assessment.logID())
                .param("recordID", assessment.recordID())
                .param("overallRiskScore", assessment.overallRiskScore())
                .param("initialRiskLevel", assessment.initialRiskLevel())
                .param("overallRiskLevel", assessment.overallRiskLevel())
                .param("typeOfLogicApplied", assessment.typeOfLogicApplied())
                .param("smeRiskAssessment", assessment.smeRiskAssessment())
                .update(keyHolder);
        return ((Number) keyHolder.getKeys().get("ASSESSMENTID")).longValue();
    }

    public void saveDetails(List<RiskAssessmentDetail> details) {
        for (RiskAssessmentDetail detail : details) {
            jdbcClient.sql(
                    "INSERT INTO RiskAssessmentDetails (AssessmentID, RiskType, ElementName, ElementValue, RiskScore, Flag, LocalRuleApplied) VALUES (:assessmentID, :riskType, :elementName, :elementValue, :riskScore, :flag, :localRuleApplied)")
                    .param("assessmentID", detail.assessmentID())
                    .param("riskType", detail.riskType())
                    .param("elementName", detail.elementName())
                    .param("elementValue", detail.elementValue())
                    .param("riskScore", detail.riskScore())
                    .param("flag", detail.flag())
                    .param("localRuleApplied", detail.localRuleApplied())
                    .update();
        }
    }

    public void updateLog(Long logID, String responseJSON, String status) {
        jdbcClient.sql(
                "UPDATE RiskAssessmentLogs SET ResponseJSON = :responseJSON, Status = :status WHERE LogID = :logID")
                .param("logID", logID)
                .param("responseJSON", responseJSON)
                .param("status", status)
                .update();
    }

    public List<RiskAssessmentLog> findAllLogs() {
        return jdbcClient.sql("SELECT * FROM RiskAssessmentLogs")
                .query(RiskAssessmentLog.class)
                .list();
    }

    public List<RiskAssessment> findAllAssessments() {
        return jdbcClient.sql("SELECT * FROM RiskAssessments")
                .query(RiskAssessment.class)
                .list();
    }

    public List<RiskAssessment> findAssessmentsByRecordId(String recordId) {
        return jdbcClient.sql("SELECT * FROM RiskAssessments WHERE RecordID = :recordID ORDER BY CreatedAt DESC")
                .param("recordID", recordId)
                .query(RiskAssessment.class)
                .list();
    }

    public List<RiskAssessmentDetail> findDetailsByAssessmentId(Long assessmentId) {
        return jdbcClient.sql("SELECT * FROM RiskAssessmentDetails WHERE AssessmentID = :assessmentId")
                .param("assessmentId", assessmentId)
                .query(RiskAssessmentDetail.class)
                .list();
    }
}
