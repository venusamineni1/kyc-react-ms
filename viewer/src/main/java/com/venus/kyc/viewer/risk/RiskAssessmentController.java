package com.venus.kyc.viewer.risk;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/viewer/risk")
@Tag(name = "Viewer Risk Assessment", description = "Proxy endpoints for risk assessment operations including calculation, evaluation, and batch risk management")
public class RiskAssessmentController {

  private final RiskAssessmentService service;

  public RiskAssessmentController(RiskAssessmentService service) {
    this.service = service;
  }

  @Operation(summary = "Calculate risk", description = "Calculates a client risk rating based on entity, industry, geography, product, and channel risk pillars")
  @PostMapping("/calculate")
  public ResponseEntity<RiskDTOs.CalculateRiskResponse> calculateRisk(
      @RequestBody RiskDTOs.CalculateRiskRequest request) {
    RiskDTOs.CalculateRiskResponse response = service.calculateRisk(request);
    return ResponseEntity.ok(response);
  }

  @Operation(summary = "Evaluate risk for client", description = "Evaluates risk for a client by automatically constructing a risk request from client data")
  @PostMapping("/evaluate/{clientId}")
  public ResponseEntity<RiskDTOs.CalculateRiskResponse> evaluateRiskForClient(
      @Parameter(description = "Client ID") @org.springframework.web.bind.annotation.PathVariable Long clientId) {
    RiskDTOs.CalculateRiskResponse response = service.evaluateRiskForClient(clientId);
    return ResponseEntity.ok(response);
  }

  @Operation(summary = "Get all risk logs", description = "Returns all risk assessment log entries")
  @org.springframework.web.bind.annotation.GetMapping("/logs")
  public java.util.List<RiskAssessmentLog> getAllLogs() {
    return service.findAllLogs();
  }

  @Operation(summary = "Get all assessments", description = "Returns all risk assessment records")
  @org.springframework.web.bind.annotation.GetMapping("/assessments")
  public java.util.List<RiskAssessment> getAllAssessments() {
    return service.findAllAssessments();
  }

  @Operation(summary = "Get assessments by record ID", description = "Returns risk assessments associated with a specific record ID")
  @org.springframework.web.bind.annotation.GetMapping("/assessments/{recordId}")
  public java.util.List<RiskAssessment> getAssessmentsByRecordId(
      @Parameter(description = "Record ID") @org.springframework.web.bind.annotation.PathVariable String recordId) {
    return service.getAssessmentsByRecordId(recordId);
  }

  @Operation(summary = "Get assessment details", description = "Returns the detailed pillar scores for a specific risk assessment")
  @org.springframework.web.bind.annotation.GetMapping("/assessment-details/{assessmentId}")
  public java.util.List<RiskAssessmentDetail> getAssessmentDetails(
      @Parameter(description = "Assessment ID") @org.springframework.web.bind.annotation.PathVariable Long assessmentId) {
    return service.getAssessmentDetails(assessmentId);
  }

  // Batch Risk Endpoints

  @Operation(summary = "Get batch risk mapping", description = "Returns the configured batch risk field mapping")
  @org.springframework.web.bind.annotation.GetMapping("/batch/mapping")
  public ResponseEntity<java.util.List<java.util.Map<String, Object>>> getBatchMapping() {
    return ResponseEntity.ok(service.getBatchMapping());
  }

  @Operation(summary = "Update batch risk mapping", description = "Saves or updates the batch risk field mapping configuration")
  @PostMapping("/batch/mapping")
  public ResponseEntity<Void> updateBatchMapping(
      @RequestBody java.util.List<java.util.Map<String, Object>> configs) {
    service.updateBatchMapping(configs);
    return ResponseEntity.ok().build();
  }

  @Operation(summary = "Generate batch test JSON", description = "Generates a test JSONL payload for a single client using current mapping configuration")
  @PostMapping("/batch/test-generate")
  public ResponseEntity<String> generateBatchTestJson(
      @RequestBody java.util.Map<String, Object> clientData) {
    return ResponseEntity.ok(service.generateBatchTestJson(clientData));
  }
}
