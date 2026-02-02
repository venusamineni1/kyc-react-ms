package com.venus.kyc.risk;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RiskDTOs {

        // REQUEST DTOs

        public record CalculateRiskRequest(
                        @JsonProperty("header") Header header,
                        @JsonProperty("clientRiskRatingRequest") List<ClientRiskRatingRequestItem> clientRiskRatingRequest) {
        }

        public record Header(
                        String callerSystem,
                        String dbBusinessline,
                        String crrmVersion,
                        String requestID,
                        String requestTimeStamp,
                        // Response specific fields
                        String responseTimeStamp,
                        String eventModelRunInstance,
                        String eventModelVersion,
                        String eventCalibrationVersion) {
        }

        public record ClientRiskRatingRequestItem(
                        @JsonProperty("clientDetails") ClientDetails clientDetails,
                        @JsonProperty("entityRiskType") EntityRiskType entityRiskType,
                        @JsonProperty("industryRiskType") IndustryRiskType industryRiskType,
                        @JsonProperty("geoRiskType") GeoRiskType geoRiskType,
                        @JsonProperty("productRiskType") List<ProductRiskType> productRiskType,
                        @JsonProperty("channelRiskType") ChannelRiskType channelRiskType) {
        }

        public record ClientDetails(
                        String defenceRevenue,
                        String recordID,
                        String clientAdoptionCountry,
                        String smeAssessment,
                        String smeRiskAssessment,
                        @JsonProperty("additionalRule") List<AdditionalRule> additionalRule) {
        }

        public record AdditionalRule(
                        String ruleType,
                        String question,
                        String response) {
        }

        public record EntityRiskType(
                        String typeKYCLegalEntityCode,
                        // Response fields
                        Integer pillarScore,
                        String pillarRiskCategory,
                        String typeOfLogicApplied,
                        @JsonProperty("riskClassification") List<RiskClassification> riskClassification) {
        }

        public record IndustryRiskType(
                        @JsonProperty("occupationCode") List<String> occupationCode,
                        // Response fields
                        Integer pillarScore,
                        String pillarRiskCategory,
                        String typeOfLogicApplied,
                        @JsonProperty("riskClassification") List<RiskClassification> riskClassification) {
        }

        public record GeoRiskType(
                        @JsonProperty("relatedParty") List<RelatedPartyRisk> relatedParty,
                        @JsonProperty("partyAccount") List<PartyAccountRisk> partyAccount,
                        // Response fields
                        Integer pillarScore,
                        String pillarRiskCategory,
                        String typeOfLogicApplied,
                        @JsonProperty("riskClassification") List<RiskClassification> riskClassification) {
        }

        public record RelatedPartyRisk(
                        @JsonProperty("relatedPartyElementValues") List<String> relatedPartyElementValues,
                        String relatedPartyElement) {
        }

        public record PartyAccountRisk(
                        @JsonProperty("countryOfNationality") List<String> countryOfNationality,
                        @JsonProperty("originOfFunds") List<String> originOfFunds,
                        @JsonProperty("dateOfResidence") List<String> dateOfResidence,
                        @JsonProperty("addressType") AddressTypeRisk addressType) {
        }

        public record AddressTypeRisk(
                        @JsonProperty("postalAddress") List<String> postalAddress,
                        String clientDomicile) {
        }

        public record ProductRiskType(
                        String productCode,
                        // Response fields
                        Integer pillarScore,
                        String pillarRiskCategory,
                        String typeOfLogicApplied,
                        @JsonProperty("riskClassification") List<RiskClassification> riskClassification) {
        }

        public record ChannelRiskType(
                        String channelCode,
                        // Response fields
                        Integer pillarScore,
                        String pillarRiskCategory,
                        String typeOfLogicApplied,
                        @JsonProperty("riskClassification") List<RiskClassification> riskClassification) {
        }

        // RESPONSE DTOs

        public record CalculateRiskResponse(
                        @JsonProperty("header") Header header,
                        @JsonProperty("processStatus") ProcessStatus processStatus,
                        @JsonProperty("clientRiskRatingResponse") List<ClientRiskRatingResponseItem> clientRiskRatingResponse) {
        }

        public record ProcessStatus(
                        String crreStatus,
                        Integer successfulRecords,
                        Integer errorRecords,
                        Integer warningRecords) {
        }

        public record ClientRiskRatingResponseItem(
                        String recordID,
                        String riskRatingType,
                        String clientAdoptionCountry,
                        String error,
                        @JsonProperty("overallRiskAssessment") OverallRiskAssessment overallRiskAssessment,
                        @JsonProperty("entityRiskType") EntityRiskType entityRiskType,
                        @JsonProperty("industryRiskType") IndustryRiskType industryRiskType,
                        @JsonProperty("geoRiskType") GeoRiskType geoRiskType,
                        @JsonProperty("productRiskType") ProductRiskType productRiskType, // Note: Response is Object,
                                                                                          // Request was
                                                                                          // Array. The wrapper should
                                                                                          // handle this.
                                                                                          // Wait, example shows object
                                                                                          // in response.
                        @JsonProperty("channelRiskType") ChannelRiskType channelRiskType) {
        }

        public record OverallRiskAssessment(
                        String riskScoreDetails,
                        Integer overallRiskScore,
                        String initialRiskLevel,
                        String riskRatingPreSMEAssessment,
                        String overallRiskLevel,
                        String typeOfLogicApplied,
                        String smeRiskAssessment) {
        }

        public record RiskClassification(
                        String elementName,
                        String elementValue,
                        Integer riskScore,
                        String flag,
                        String regulatoryCRROverride,
                        String localRuleApplied) {
        }
}
