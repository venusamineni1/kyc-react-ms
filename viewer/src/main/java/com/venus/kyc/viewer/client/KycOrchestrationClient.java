package com.venus.kyc.viewer.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Client to call the KYC Orchestration Service.
 *
 * Delegates screening and risk assessment to the dedicated orchestration service,
 * which handles:
 * - Parallel/sequential execution of screening and risk calls
 * - PII encryption for sensitive data
 * - Audit trail tracking
 * - Webhook notifications
 * - Soft-fail strategies
 */
@Component
public class KycOrchestrationClient {

    private static final Logger log = LoggerFactory.getLogger(KycOrchestrationClient.class);

    private final RestClient restClient;

    @Value("${orchestration.service.url:http://localhost:8084}")
    private String orchestrationServiceUrl;

    @Value("${internal.api.key:dev-internal-kyc-key-change-in-prod}")
    private String internalApiKey;

    public KycOrchestrationClient(RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * Initiates a KYC pre-check which orchestrates screening and risk assessment.
     *
     * @param request KYC precheck request with client details
     * @return KycPrecheckResponse with APPROVED or ON_HOLD status
     */
    public KycPrecheckResponse initiatePrecheck(KycPrecheckRequest request) {
        String url = orchestrationServiceUrl + "/api/v1/kyc/initiate";

        try {
            log.info("Initiating KYC precheck for client: {}", request.getUniqueClientID());

            KycPrecheckResponse response = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Internal-Api-Key", internalApiKey)
                    .body(request)
                    .retrieve()
                    .body(KycPrecheckResponse.class);

            log.info("KYC precheck completed for client: {} - Status: {}",
                    request.getUniqueClientID(), response.getKycStatus());

            return response;
        } catch (Exception e) {
            log.error("Failed to call KYC Orchestration Service at {}", url, e);
            throw new RuntimeException("Failed to orchestrate KYC precheck: " + e.getMessage(), e);
        }
    }

    /**
     * Data Transfer Object for KYC precheck request.
     * Maps to com.venus.kyc.orchestration.dto.KycPrecheckRequest
     */
    public static class KycPrecheckRequest {
        private String uniqueClientID;
        private String firstName;
        private String middleName;
        private String lastName;
        private String title;
        private String dob;
        private String businessLine;
        private String cityOfBirth;
        private String countryOfBirth;
        private String primaryCitizenship;
        private String secondCitizenship;
        private String countryOfResidence;
        private String occupation;
        private ResidentialAddress residentialAddress;
        private String typeOfLegitimizationDocument;
        private String issuingAuthority;
        private String identificationNumber;
        private String expirationDate;
        private String germanTaxID;
        private String webhookUrl;

        // Getters
        public String getUniqueClientID() { return uniqueClientID; }
        public String getFirstName() { return firstName; }
        public String getLastName() { return lastName; }
        public String getDob() { return dob; }
        public String getBusinessLine() { return businessLine; }
        public String getCityOfBirth() { return cityOfBirth; }
        public String getCountryOfBirth() { return countryOfBirth; }
        public String getPrimaryCitizenship() { return primaryCitizenship; }
        public String getSecondCitizenship() { return secondCitizenship; }
        public String getCountryOfResidence() { return countryOfResidence; }
        public String getOccupation() { return occupation; }
        public ResidentialAddress getResidentialAddress() { return residentialAddress; }
        public String getTypeOfLegitimizationDocument() { return typeOfLegitimizationDocument; }
        public String getIssuingAuthority() { return issuingAuthority; }
        public String getIdentificationNumber() { return identificationNumber; }
        public String getExpirationDate() { return expirationDate; }
        public String getGermanTaxID() { return germanTaxID; }
        public String getWebhookUrl() { return webhookUrl; }

        // Setters - Builder pattern
        public KycPrecheckRequest setUniqueClientID(String v) { this.uniqueClientID = v; return this; }
        public KycPrecheckRequest setFirstName(String v) { this.firstName = v; return this; }
        public KycPrecheckRequest setMiddleName(String v) { this.middleName = v; return this; }
        public KycPrecheckRequest setLastName(String v) { this.lastName = v; return this; }
        public KycPrecheckRequest setTitle(String v) { this.title = v; return this; }
        public KycPrecheckRequest setDob(String v) { this.dob = v; return this; }
        public KycPrecheckRequest setBusinessLine(String v) { this.businessLine = v; return this; }
        public KycPrecheckRequest setCityOfBirth(String v) { this.cityOfBirth = v; return this; }
        public KycPrecheckRequest setCountryOfBirth(String v) { this.countryOfBirth = v; return this; }
        public KycPrecheckRequest setPrimaryCitizenship(String v) { this.primaryCitizenship = v; return this; }
        public KycPrecheckRequest setSecondCitizenship(String v) { this.secondCitizenship = v; return this; }
        public KycPrecheckRequest setCountryOfResidence(String v) { this.countryOfResidence = v; return this; }
        public KycPrecheckRequest setOccupation(String v) { this.occupation = v; return this; }
        public KycPrecheckRequest setResidentialAddress(ResidentialAddress v) { this.residentialAddress = v; return this; }
        public KycPrecheckRequest setTypeOfLegitimizationDocument(String v) { this.typeOfLegitimizationDocument = v; return this; }
        public KycPrecheckRequest setIssuingAuthority(String v) { this.issuingAuthority = v; return this; }
        public KycPrecheckRequest setIdentificationNumber(String v) { this.identificationNumber = v; return this; }
        public KycPrecheckRequest setExpirationDate(String v) { this.expirationDate = v; return this; }
        public KycPrecheckRequest setGermanTaxID(String v) { this.germanTaxID = v; return this; }
        public KycPrecheckRequest setWebhookUrl(String v) { this.webhookUrl = v; return this; }
    }

    /**
     * Data Transfer Object for KYC precheck response.
     * Maps to com.venus.kyc.orchestration.dto.KycPrecheckResponse
     */
    public static class KycPrecheckResponse {
        private String kycId;
        private String kycStatus;  // "APPROVED" or "ON_HOLD"
        private String screeningResult;  // "Hit" or "NoHit"
        private java.util.List<String> hitContext;
        private String riskRating;  // "LOW", "MEDIUM", "HIGH", "UNKNOWN"
        private Integer riskScore;  // Numeric overall risk score (1-100)
        private String userId;
        private String screeningRequestId;  // Screening request ID from orchestration
        private String riskRequestId;  // Risk request ID from orchestration

        // Getters
        public String getKycId() { return kycId; }
        public String getKycStatus() { return kycStatus; }
        public String getScreeningResult() { return screeningResult; }
        public java.util.List<String> getHitContext() { return hitContext; }
        public String getRiskRating() { return riskRating; }
        public Integer getRiskScore() { return riskScore; }
        public String getUserId() { return userId; }
        public String getScreeningRequestId() { return screeningRequestId; }
        public String getRiskRequestId() { return riskRequestId; }

        // Setters
        public KycPrecheckResponse setKycId(String v) { this.kycId = v; return this; }
        public KycPrecheckResponse setKycStatus(String v) { this.kycStatus = v; return this; }
        public KycPrecheckResponse setScreeningResult(String v) { this.screeningResult = v; return this; }
        public KycPrecheckResponse setHitContext(java.util.List<String> v) { this.hitContext = v; return this; }
        public KycPrecheckResponse setRiskRating(String v) { this.riskRating = v; return this; }
        public KycPrecheckResponse setRiskScore(Integer v) { this.riskScore = v; return this; }
        public KycPrecheckResponse setUserId(String v) { this.userId = v; return this; }
        public KycPrecheckResponse setScreeningRequestId(String v) { this.screeningRequestId = v; return this; }
        public KycPrecheckResponse setRiskRequestId(String v) { this.riskRequestId = v; return this; }
    }

    /**
     * Residential address for client
     */
    public static class ResidentialAddress {
        private String addressLine1;
        private String addressLine2;
        private String city;
        private String zip;

        public String getAddressLine1() { return addressLine1; }
        public String getAddressLine2() { return addressLine2; }
        public String getCity() { return city; }
        public String getZip() { return zip; }

        public ResidentialAddress setAddressLine1(String v) { this.addressLine1 = v; return this; }
        public ResidentialAddress setAddressLine2(String v) { this.addressLine2 = v; return this; }
        public ResidentialAddress setCity(String v) { this.city = v; return this; }
        public ResidentialAddress setZip(String v) { this.zip = v; return this; }
    }
}
