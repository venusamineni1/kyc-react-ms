package com.venus.kyc.viewer.risk;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/risk")
public class RiskAssessmentController {

  private final RiskAssessmentService service;

  public RiskAssessmentController(RiskAssessmentService service) {
    this.service = service;
  }

  @PostMapping("/calculate")
  public ResponseEntity<RiskDTOs.CalculateRiskResponse> calculateRisk(
      @RequestBody RiskDTOs.CalculateRiskRequest request) {
    RiskDTOs.CalculateRiskResponse response = service.calculateRisk(request);
    return ResponseEntity.ok(response);
  }

  @PostMapping("/evaluate/{clientId}")
  public ResponseEntity<RiskDTOs.CalculateRiskResponse> evaluateRiskForClient(
      @org.springframework.web.bind.annotation.PathVariable Long clientId) {
    RiskDTOs.CalculateRiskResponse response = service.evaluateRiskForClient(clientId);
    return ResponseEntity.ok(response);
  }

  @org.springframework.web.bind.annotation.GetMapping("/logs")
  public java.util.List<RiskAssessmentLog> getAllLogs() {
    return service.findAllLogs();
  }

  @org.springframework.web.bind.annotation.GetMapping("/assessments")
  public java.util.List<RiskAssessment> getAllAssessments() {
    return service.findAllAssessments();
  }

  @org.springframework.web.bind.annotation.GetMapping("/assessments/{recordId}")
  public java.util.List<RiskAssessment> getAssessmentsByRecordId(
      @org.springframework.web.bind.annotation.PathVariable String recordId) {
    return service.getAssessmentsByRecordId(recordId);
  }

  @org.springframework.web.bind.annotation.GetMapping("/assessment-details/{assessmentId}")
  public java.util.List<RiskAssessmentDetail> getAssessmentDetails(
      @org.springframework.web.bind.annotation.PathVariable Long assessmentId) {
    return service.getAssessmentDetails(assessmentId);
  }
}
