package com.venus.kyc.risk;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/internal/risk", "/api/v1/internal/risk"})
@Tag(name = "Risk Assessment", description = "Endpoints for calculating client risk ratings using entity, industry, geo, product, and channel risk pillars")
public class RiskController {

    private final RiskService riskService;

    public RiskController(RiskService riskService) {
        this.riskService = riskService;
    }

    @Operation(
        summary = "Calculate client risk rating",
        description = "Submits a risk rating request to the CRRE engine and stores the assessment results including entity, industry, geo, product, and channel risk scores")
    @PostMapping("/calculate")
    public RiskDTOs.CalculateRiskResponse calculateRisk(
            @RequestBody RiskDTOs.CalculateRiskRequest request) {
        return riskService.calculateRisk(request);
    }

    @Operation(
        summary = "Get all risk assessment logs",
        description = "Returns the full history of risk assessment API calls including request/response payloads and status")
    @GetMapping("/logs")
    public List<RiskAssessmentLog> getLogs() {
        return riskService.getLogs();
    }

    @Operation(
        summary = "Get all risk assessments",
        description = "Returns all stored risk assessment results with overall risk scores and levels")
    @GetMapping("/assessments")
    public List<RiskAssessment> getAllAssessments() {
        return riskService.getAllAssessments();
    }

    @Operation(
        summary = "Get assessments by record ID",
        description = "Returns risk assessments for a specific client record ID")
    @GetMapping("/assessments/{recordId}")
    public List<RiskAssessment> getAssessmentsByRecordId(
            @Parameter(description = "Client record identifier") @PathVariable String recordId) {
        return riskService.getAssessmentsByRecordId(recordId);
    }

    @Operation(
        summary = "Get assessment details",
        description = "Returns the detailed risk classification breakdown for a specific assessment, including individual pillar scores")
    @GetMapping("/assessment-details/{assessmentId}")
    public List<RiskAssessmentDetail> getAssessmentDetails(
            @Parameter(description = "Assessment ID") @PathVariable Long assessmentId) {
        return riskService.getAssessmentDetails(assessmentId);
    }
}
