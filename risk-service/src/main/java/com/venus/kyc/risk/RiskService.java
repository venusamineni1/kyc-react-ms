package com.venus.kyc.risk;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates the risk calculation lifecycle:
 *   1. Serialize and persist the inbound request as an audit log
 *   2. Delegate to the configured RiskProvider (mock or real CRRE)
 *   3. Persist assessment results and per-pillar details
 *   4. Return the provider response
 *
 * Also exposes read-only query methods used by the controller.
 */
@Service
public class RiskService {

    private static final Logger log = LoggerFactory.getLogger(RiskService.class);

    private final RiskAssessmentRepository repository;
    private final ObjectMapper objectMapper;
    private final RiskProvider riskProvider;

    public RiskService(RiskAssessmentRepository repository,
                       ObjectMapper objectMapper,
                       RiskProvider riskProvider) {
        this.repository   = repository;
        this.objectMapper = objectMapper;
        this.riskProvider = riskProvider;
    }

    // ── Calculate ─────────────────────────────────────────────────────────────

    public RiskDTOs.CalculateRiskResponse calculateRisk(RiskDTOs.CalculateRiskRequest request) {
        // 1. Persist request audit log (fixes the old "{}" bug)
        String requestJson = serialize(request);
        Long logId = repository.saveLog(new RiskAssessmentLog(null, requestJson, null, "PENDING", null));
        log.info("Risk calculation initiated — logId={}", logId);

        RiskDTOs.CalculateRiskResponse response;
        try {
            // 2. Call provider (mock or real CRRE via mTLS)
            response = riskProvider.calculateRisk(request);

            // 3. Persist results
            String responseJson = serialize(response);
            repository.updateLog(logId, responseJson, "SUCCESS");
            persistResults(logId, response);

            log.info("Risk calculation completed — logId={}, status={}",
                    logId,
                    response.processStatus() != null ? response.processStatus().crreStatus() : "unknown");

        } catch (Exception e) {
            log.error("Risk calculation failed — logId={}", logId, e);
            repository.updateLog(logId, "Error: " + e.getMessage(), "ERROR");
            throw new RuntimeException("Risk calculation failed", e);
        }

        return response;
    }

    // ── Read-only queries ─────────────────────────────────────────────────────

    public List<RiskAssessmentLog> getLogs() {
        return repository.findAllLogs();
    }

    public List<RiskAssessment> getAllAssessments() {
        return repository.findAllAssessments();
    }

    public List<RiskAssessment> getAssessmentsByRecordId(String recordId) {
        return repository.findAssessmentsByRecordId(recordId);
    }

    public List<RiskAssessmentDetail> getAssessmentDetails(Long assessmentId) {
        return repository.findDetailsByAssessmentId(assessmentId);
    }

    // ── Persistence helpers ───────────────────────────────────────────────────

    private void persistResults(Long logId, RiskDTOs.CalculateRiskResponse response) {
        if (response == null || response.clientRiskRatingResponse() == null) return;

        for (RiskDTOs.ClientRiskRatingResponseItem item : response.clientRiskRatingResponse()) {
            RiskDTOs.OverallRiskAssessment overall = item.overallRiskAssessment();

            RiskAssessment assessment = new RiskAssessment(
                    null, logId, item.recordID(),
                    overall != null ? overall.overallRiskScore()    : 0,
                    overall != null ? overall.initialRiskLevel()    : null,
                    overall != null ? overall.overallRiskLevel()    : null,
                    overall != null ? overall.typeOfLogicApplied()  : null,
                    overall != null ? overall.smeRiskAssessment()   : null,
                    null);
            Long assessmentId = repository.saveAssessment(assessment);

            repository.saveDetails(buildDetails(assessmentId, item));
        }
    }

    private List<RiskAssessmentDetail> buildDetails(Long assessmentId,
                                                     RiskDTOs.ClientRiskRatingResponseItem item) {
        List<RiskAssessmentDetail> details = new ArrayList<>();

        addPillarDetails(details, assessmentId, "Entity",
                item.entityRiskType()   != null ? item.entityRiskType().riskClassification()   : null);
        addPillarDetails(details, assessmentId, "Industry",
                item.industryRiskType() != null ? item.industryRiskType().riskClassification() : null);
        addPillarDetails(details, assessmentId, "Geographic",
                item.geoRiskType()      != null ? item.geoRiskType().riskClassification()      : null);
        addPillarDetails(details, assessmentId, "Product",
                item.productRiskType()  != null ? item.productRiskType().riskClassification()  : null);
        addPillarDetails(details, assessmentId, "Channel",
                item.channelRiskType()  != null ? item.channelRiskType().riskClassification()  : null);

        return details;
    }

    private void addPillarDetails(List<RiskAssessmentDetail> details, Long assessmentId,
                                   String pillarType, List<RiskDTOs.RiskClassification> classifications) {
        if (classifications == null) return;
        for (RiskDTOs.RiskClassification rc : classifications) {
            details.add(new RiskAssessmentDetail(
                    null, assessmentId, pillarType,
                    rc.elementName(), rc.elementValue(),
                    rc.riskScore(), rc.flag(), rc.localRuleApplied()));
        }
    }

    private String serialize(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize object for audit log", e);
            return "{}";
        }
    }
}
