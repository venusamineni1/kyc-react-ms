package com.venus.kyc.risk;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.Collections;

@RestController
@RequestMapping("/api/internal/risk")
@Tag(name = "Risk Assessment", description = "Endpoints for calculating client risk ratings using entity, industry, geo, product, and channel risk pillars")
public class RiskController {

  private final RiskAssessmentRepository repository;
  private final RestClient restClient;

  @Value("${risk.external-api.url:http://localhost:8081/api/internal/risk/dummy-external-api}")
  private String externalApiUrl;

  public RiskController(
          RiskAssessmentRepository repository,
          RestClient.Builder restClientBuilder,
          @Value("${internal.api.key}") String internalApiKey) {
    this.repository = repository;
    this.restClient = restClientBuilder
            .defaultHeader("X-Internal-Api-Key", internalApiKey)
            .build();
  }

  @Operation(summary = "Calculate client risk rating", description = "Submits a risk rating request to the external CRRE engine and stores the assessment results including entity, industry, geo, product, and channel risk scores")
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
          // Geographic Risk
          if (item.geoRiskType() != null && item.geoRiskType().riskClassification() != null) {
            for (RiskDTOs.RiskClassification rc : item.geoRiskType().riskClassification()) {
              details.add(createDetail(assessmentId, "Geographic", rc));
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

  @Operation(summary = "Get all risk assessment logs", description = "Returns the full history of risk assessment API calls including request/response payloads and status")
  @GetMapping("/logs")
  public java.util.List<RiskAssessmentLog> getLogs() {
    return repository.findAllLogs();
  }

  @Operation(summary = "Get all risk assessments", description = "Returns all stored risk assessment results with overall risk scores and levels")
  @GetMapping("/assessments")
  public java.util.List<RiskAssessment> getAllAssessments() {
    return repository.findAllAssessments();
  }

  @Operation(summary = "Get assessments by record ID", description = "Returns risk assessments for a specific client record ID")
  @GetMapping("/assessments/{recordId}")
  public java.util.List<RiskAssessment> getAssessmentsByRecordId(
      @Parameter(description = "Client record identifier") @PathVariable String recordId) {
    return repository.findAssessmentsByRecordId(recordId);
  }

  @Operation(summary = "Get assessment details", description = "Returns the detailed risk classification breakdown for a specific assessment, including individual pillar scores")
  @GetMapping("/assessment-details/{assessmentId}")
  public java.util.List<RiskAssessmentDetail> getAssessmentDetails(
      @Parameter(description = "Assessment ID") @PathVariable Long assessmentId) {
    return repository.findDetailsByAssessmentId(assessmentId);
  }

  @Operation(summary = "Mock external CRRE API", description = "Simulates the external risk rating engine response for development and testing purposes")
  @PostMapping("/dummy-external-api")
  public org.springframework.http.ResponseEntity<String> mockExternalApi(@RequestBody String request) {
    String recordId = "00001497165";
    boolean isCuba = request != null && (request.contains("\"CU\"") || request.toLowerCase().contains("\"cuba\""));
    String riskLevel = isCuba ? "HIGH" : "LOW";
    int riskScore = isCuba ? 9 : 1;
    // Pillar scores — proportional to overall risk so the radar chart is meaningful
    int pEntity   = isCuba ? 82 : 14;
    int pIndustry = isCuba ? 75 : 18;
    int pGeo      = isCuba ? 91 : 12;
    int pProduct  = isCuba ? 70 : 10;
    int pChannel  = isCuba ? 85 : 8;

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
                    "overallRiskScore": %d,
                    "initialRiskLevel": "%s",
                    "riskRatingPreSMEAssessment": "%s",
                    "overallRiskLevel": "%s",
                    "typeOfLogicApplied": "Standard",
                    "smeRiskAssessment": ""
                  },
                  "entityRiskType": {
                    "pillarScore": %d,
                    "pillarRiskCategory": "%s",
                    "typeOfLogicApplied": "",
                    "riskClassification": [
                      {
                        "elementName": "typeKYCLegalEntityCode",
                        "elementValue": "NP4",
                        "riskScore": %d,
                        "flag": null,
                        "regulatoryCRROverride": null,
                        "localRuleApplied": "N"
                      }
                    ]
                  },
                  "industryRiskType": {
                    "pillarScore": %d,
                    "pillarRiskCategory": "%s",
                    "typeOfLogicApplied": null,
                    "riskClassification": [
                      {
                        "elementName": "occupationCode",
                        "elementValue": "00101",
                        "riskScore": %d,
                        "flag": null,
                        "regulatoryCRROverride": null,
                        "localRuleApplied": "N"
                      }
                    ]
                  },
                  "geoRiskType": {
                    "pillarScore": %d,
                    "pillarRiskCategory": "%s",
                    "typeOfLogicApplied": null,
                    "riskClassification": [
                      {
                        "elementName": "countryOfNationality",
                        "elementValue": "DE",
                        "riskScore": %d,
                        "flag": null,
                        "regulatoryCRROverride": null,
                        "localRuleApplied": "N"
                      },
                      {
                        "elementName": "originOfFunds",
                        "elementValue": "DE",
                        "riskScore": %d,
                        "flag": null,
                        "regulatoryCRROverride": null,
                        "localRuleApplied": "N"
                      },
                      {
                        "elementName": "clientDomicile",
                        "elementValue": "DE",
                        "riskScore": %d,
                        "flag": null,
                        "regulatoryCRROverride": null,
                        "localRuleApplied": "N"
                      }
                    ]
                  },
                  "productRiskType": {
                    "pillarScore": %d,
                    "pillarRiskCategory": "%s",
                    "typeOfLogicApplied": null,
                    "riskClassification": [
                      {
                        "elementName": "productCode",
                        "elementValue": "OAP1",
                        "riskScore": %d,
                        "flag": null,
                        "regulatoryCRROverride": null,
                        "localRuleApplied": "N"
                      }
                    ]
                  },
                  "channelRiskType": {
                    "pillarScore": %d,
                    "pillarRiskCategory": "%s",
                    "typeOfLogicApplied": null,
                    "riskClassification": [
                      {
                        "elementName": "channelCode",
                        "elementValue": "CHN05",
                        "riskScore": %d,
                        "flag": null,
                        "regulatoryCRROverride": null,
                        "localRuleApplied": "N"
                      }
                    ]
                  }
                }
              ]
            }
        """.formatted(
            recordId, riskScore, riskLevel, riskLevel, riskLevel,
            // entity
            pEntity, riskLevel, pEntity,
            // industry
            pIndustry, riskLevel, pIndustry,
            // geo (pillarScore + riskCategory + 3 classification entries)
            pGeo, riskLevel, pGeo, pGeo, pGeo,
            // product
            pProduct, riskLevel, pProduct,
            // channel
            pChannel, riskLevel, pChannel
        );
    return org.springframework.http.ResponseEntity.ok()
        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
        .body(jsonResponse);
  }
}
