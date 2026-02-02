package com.venus.kyc.risk;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.Collections;

@RestController
@RequestMapping("/api/internal/risk")
public class RiskController {

  private final RiskAssessmentRepository repository;
  private final RestClient restClient;

  @Value("${risk.external-api.url:http://localhost:8081/api/internal/risk/dummy-external-api}")
  private String externalApiUrl;

  public RiskController(RiskAssessmentRepository repository) {
    this.repository = repository;
    this.restClient = RestClient.create();
  }

  @PostMapping("/calculate")
  public RiskDTOs.CalculateRiskResponse calculateRisk(@RequestBody RiskDTOs.CalculateRiskRequest request) {
    // Log Request
    String requestJson = "{}"; // In real usage, use ObjectMapper
    Long logId = repository.saveLog(new RiskAssessmentLog(null, requestJson, null, "PENDING", null));

    // Call External API (Dummy for now, or actual external vendor)
    // For Phase 1, we still rely on the 'Dummy External API' likely hosted in the
    // Monolith or mocked here?
    // Let's assume the Monolith is still the "Mock Vendor" provider for now, or we
    // define the Mock here.
    // Actually, the original Service called `API_URL`.

    RiskDTOs.CalculateRiskResponse response = null;
    try {
      response = restClient.post()
          .uri(externalApiUrl)
          .contentType(MediaType.APPLICATION_JSON)
          .body(request)
          .retrieve()
          .body(RiskDTOs.CalculateRiskResponse.class);

      // Log Success
      repository.updateLog(logId, "{}", "SUCCESS");

      // Save Results
      if (response != null && response.clientRiskRatingResponse() != null) {
        for (RiskDTOs.ClientRiskRatingResponseItem item : response.clientRiskRatingResponse()) {
          RiskAssessment assessment = new RiskAssessment(
              null, logId, item.recordID(),
              item.overallRiskAssessment() != null ? item.overallRiskAssessment().overallRiskScore() : 0,
              item.overallRiskAssessment() != null ? item.overallRiskAssessment().initialRiskLevel()
                  : null,
              item.overallRiskAssessment() != null ? item.overallRiskAssessment().overallRiskLevel()
                  : null,
              item.overallRiskAssessment() != null ? item.overallRiskAssessment().typeOfLogicApplied()
                  : null,
              item.overallRiskAssessment() != null ? item.overallRiskAssessment().smeRiskAssessment()
                  : null,
              null);
          Long assessmentId = repository.saveAssessment(assessment);

          java.util.List<RiskAssessmentDetail> details = new java.util.ArrayList<>();

          // Entity Risk
          if (item.entityRiskType() != null && item.entityRiskType().riskClassification() != null) {
            for (RiskDTOs.RiskClassification rc : item.entityRiskType().riskClassification()) {
              details.add(createDetail(assessmentId, "Entity", rc));
            }
          }
          // Industry Risk
          if (item.industryRiskType() != null && item.industryRiskType().riskClassification() != null) {
            for (RiskDTOs.RiskClassification rc : item.industryRiskType().riskClassification()) {
              details.add(createDetail(assessmentId, "Industry", rc));
            }
          }
          // Geo Risk
          if (item.geoRiskType() != null && item.geoRiskType().riskClassification() != null) {
            for (RiskDTOs.RiskClassification rc : item.geoRiskType().riskClassification()) {
              details.add(createDetail(assessmentId, "Geo", rc));
            }
          }
          // Product Risk
          if (item.productRiskType() != null && item.productRiskType().riskClassification() != null) {
            for (RiskDTOs.RiskClassification rc : item.productRiskType().riskClassification()) {
              details.add(createDetail(assessmentId, "Product", rc));
            }
          }
          // Channel Risk
          if (item.channelRiskType() != null && item.channelRiskType().riskClassification() != null) {
            for (RiskDTOs.RiskClassification rc : item.channelRiskType().riskClassification()) {
              details.add(createDetail(assessmentId, "Channel", rc));
            }
          }

          repository.saveDetails(details);
        }
      }

    } catch (Exception e) {
      repository.updateLog(logId, "Error: " + e.getMessage(), "ERROR");
      throw new RuntimeException("Risk Calculation Failed", e);
    }

    return response;
  }

  private RiskAssessmentDetail createDetail(Long assessmentId, String type, RiskDTOs.RiskClassification rc) {
    return new RiskAssessmentDetail(
        null, assessmentId, type, rc.elementName(), rc.elementValue(), rc.riskScore(), rc.flag(),
        rc.localRuleApplied());
  }

  @GetMapping("/logs")
  public java.util.List<RiskAssessmentLog> getLogs() {
    return repository.findAllLogs();
  }

  @GetMapping("/assessments")
  public java.util.List<RiskAssessment> getAllAssessments() {
    return repository.findAllAssessments();
  }

  @GetMapping("/assessments/{recordId}")
  public java.util.List<RiskAssessment> getAssessmentsByRecordId(@PathVariable String recordId) {
    return repository.findAssessmentsByRecordId(recordId);
  }

  @GetMapping("/assessment-details/{assessmentId}")
  public java.util.List<RiskAssessmentDetail> getAssessmentDetails(@PathVariable Long assessmentId) {
    return repository.findDetailsByAssessmentId(assessmentId);
  }

  @PostMapping("/dummy-external-api")
  public org.springframework.http.ResponseEntity<String> mockExternalApi(@RequestBody String request) {
    String recordId = "00001497165";
    try {
      com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
      com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(request);
      if (root.has("clientRiskRatingRequest") && root.get("clientRiskRatingRequest").isArray()
          && root.get("clientRiskRatingRequest").size() > 0) {
        com.fasterxml.jackson.databind.JsonNode firstItem = root.get("clientRiskRatingRequest").get(0);
        if (firstItem.has("clientDetails") && firstItem.get("clientDetails").has("recordID")) {
          recordId = firstItem.get("clientDetails").get("recordID").asText();
        }
      }
    } catch (Exception e) {
      // ignore parsing errors, use default
    }

    String jsonResponse = """
            {
              "header": {
                "requestID": "test",
                "callerSystem": "173471-1",
                "responseTimeStamp": "2026-01-25T05:40:10+01:00",
                "eventModelRunInstance": "114654-1",
                "eventModelVersion": "CRRE 22.2",
                "eventCalibrationVersion": "122",
                "crrmVersion": "2.0"
              },
              "processStatus": {
                "crreStatus": "Success",
                "successfulRecords": 1,
                "errorRecords": 0,
                "warningRecords": 0
              },
              "clientRiskRatingResponse": [
                {
                  "recordID": "%s",
                  "riskRatingType": null,
                  "clientAdoptionCountry": "DE",
                  "error": null,
                  "overallRiskAssessment": {
                    "riskScoreDetails": null,
                    "overallRiskScore": 1,
                    "initialRiskLevel": "LOW",
                    "riskRatingPreSMEAssessment": "HIGH",
                    "overallRiskLevel": "HIGH",
                    "typeOfLogicApplied": "Adverse Media",
                    "smeRiskAssessment": ""
                  },
                  "entityRiskType": {
                    "pillarScore": 0,
                    "pillarRiskCategory": "LOW",
                    "typeOfLogicApplied": "",
                    "riskClassification": [
                      {
                        "elementName": "typeKYCLegalEntityCode",
                        "elementValue": "NP4",
                        "riskScore": 0,
                        "flag": null,
                        "regulatoryCRROverride": null,
                        "localRuleApplied": "N"
                      }
                    ]
                  },
                  "industryRiskType": {
                    "pillarScore": 0,
                    "pillarRiskCategory": "LOW",
                    "typeOfLogicApplied": null,
                    "riskClassification": [
                      {
                        "elementName": "occupationCode",
                        "elementValue": "00101",
                        "riskScore": 0,
                        "flag": null,
                        "regulatoryCRROverride": null,
                        "localRuleApplied": "N"
                      }
                    ]
                  },
                  "geoRiskType": {
                    "pillarScore": 0,
                    "pillarRiskCategory": "LOW",
                    "typeOfLogicApplied": null,
                    "riskClassification": [
                      {
                        "elementName": "countryOfNationality",
                        "elementValue": "DE",
                        "riskScore": 0,
                        "flag": null,
                        "regulatoryCRROverride": null,
                        "localRuleApplied": "N"
                      },
                      {
                        "elementName": "originOfFunds",
                        "elementValue": "DE",
                        "riskScore": 0,
                        "flag": null,
                        "regulatoryCRROverride": null,
                        "localRuleApplied": "N"
                      },
                      {
                        "elementName": "clientDomicile",
                        "elementValue": "DE",
                        "riskScore": 0,
                        "flag": null,
                        "regulatoryCRROverride": null,
                        "localRuleApplied": "N"
                      }
                    ]
                  },
                  "productRiskType": {
                    "pillarScore": 0,
                    "pillarRiskCategory": "LOW",
                    "typeOfLogicApplied": null,
                    "riskClassification": [
                      {
                        "elementName": "productCode",
                        "elementValue": "OAP1",
                        "riskScore": 0,
                        "flag": null,
                        "regulatoryCRROverride": null,
                        "localRuleApplied": "N"
                      }
                    ]
                  },
                  "channelRiskType": {
                    "pillarScore": 1,
                    "pillarRiskCategory": "MEDIUM",
                    "typeOfLogicApplied": null,
                    "riskClassification": [
                      {
                        "elementName": "channelCode",
                        "elementValue": "CHN05",
                        "riskScore": 1,
                        "flag": null,
                        "regulatoryCRROverride": null,
                        "localRuleApplied": "N"
                      }
                    ]
                  }
                }
              ]
            }
        """.formatted(recordId);
    return org.springframework.http.ResponseEntity.ok()
        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
        .body(jsonResponse);
  }
}
