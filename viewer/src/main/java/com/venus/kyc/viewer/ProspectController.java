package com.venus.kyc.viewer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.venus.kyc.viewer.client.KycOrchestrationClient;
import com.venus.kyc.viewer.risk.RiskAssessmentRepository;
import com.venus.kyc.viewer.risk.RiskAssessmentLog;
import com.venus.kyc.viewer.risk.RiskAssessment;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/prospects")
public class ProspectController {

    private static final Logger logger = LoggerFactory.getLogger(ProspectController.class);

    private final ClientRepository clientRepository;
    private final UserAuditService userAuditService;
    private final KycOrchestrationClient kycOrchestrationClient;
    private final CaseService caseService;
    private final DocumentService documentService;
    private final RiskAssessmentRepository riskAssessmentRepository;
    private final ObjectMapper objectMapper;

    public ProspectController(ClientRepository clientRepository,
                              UserAuditService userAuditService,
                              KycOrchestrationClient kycOrchestrationClient,
                              CaseService caseService,
                              DocumentService documentService,
                              RiskAssessmentRepository riskAssessmentRepository) {
        this.clientRepository = clientRepository;
        this.userAuditService = userAuditService;
        this.kycOrchestrationClient = kycOrchestrationClient;
        this.caseService = caseService;
        this.documentService = documentService;
        this.riskAssessmentRepository = riskAssessmentRepository;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @GetMapping
    public PaginatedResponse<Client> getProspects(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        // Return masked elements if we want, but prospects data usually needs to be viewed by active analysts anyway.
        // Similar to ClientController masking logic, simplify for demo.
        return clientRepository.findProspectsPaginated(page, size);
    }

    @PostMapping(consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<Client> onboardProspect(
            @RequestPart("client") String clientJson,
            @RequestPart(value = "documents", required = false) List<MultipartFile> documents,
            @RequestParam(value = "documentTypes", required = false) List<String> documentTypes,
            @RequestParam(value = "documentComments", required = false) List<String> documentComments,
            Authentication authentication) {
        try {
            Client incomingClient = objectMapper.readValue(clientJson, Client.class);
            String username = authentication != null ? authentication.getName() : "SYSTEM";

            // Step 1: Insert Client
            Long clientId = clientRepository.insertClient(incomingClient);
            if (incomingClient.addresses() != null) {
                for (com.venus.kyc.viewer.Address addr : incomingClient.addresses()) {
                    clientRepository.addAddress(clientId, addr);
                }
            }
            userAuditService.log(username, "ONBOARDING_NEW", "Created new prospect with ID: " + clientId);

            // Step 2: Call KYC Orchestration Service
            // The orchestration service handles screening and risk assessment in parallel,
            // with proper PII encryption, audit trail, and soft-fail strategies
            KycOrchestrationClient.KycPrecheckRequest orchRequest = buildOrchestrationRequest(clientId, incomingClient);
            KycOrchestrationClient.KycPrecheckResponse orchResponse = kycOrchestrationClient.initiatePrecheck(orchRequest);

            // Store precheck results as first entry in risk assessment history
            try {
                String orchRequestJson = objectMapper.writeValueAsString(orchRequest);
                String orchResponseJson = objectMapper.writeValueAsString(orchResponse);

                RiskAssessmentLog riskLog = new RiskAssessmentLog(
                    null, // logID will be auto-generated
                    orchRequestJson,
                    orchResponseJson,
                    "COMPLETED",
                    java.time.LocalDateTime.now()
                );
                Long logId = riskAssessmentRepository.saveLog(riskLog);

                // Save the risk assessment details from orchestration
                RiskAssessment riskAssessment = new RiskAssessment(
                    null, // assessmentID will be auto-generated
                    logId,
                    "PRECHECK-" + clientId,
                    convertRiskRatingToScore(orchResponse.getRiskRating()),
                    orchResponse.getRiskRating(),
                    orchResponse.getRiskRating(),
                    "KYC_ORCHESTRATION_PRECHECK",
                    "Automated precheck from KYC orchestration service",
                    java.time.LocalDateTime.now()
                );
                riskAssessmentRepository.saveAssessment(riskAssessment);
            } catch (Exception e) {
                logger.warn("Failed to persist precheck results to risk assessment history", e);
                // Continue anyway - this is not a blocking error
            }

            userAuditService.log(username, "ONBOARDING_KYC_ORCHESTRATION",
                "KYC orchestration completed for client: " + clientId + " - Status: " + orchResponse.getKycStatus());

            // Step 3: Handle Orchestration Outcome
            if ("APPROVED".equalsIgnoreCase(orchResponse.getKycStatus())) {
                // Auto-approved: no screening hit and risk is not HIGH
                clientRepository.updateClientStatus(clientId, "APPROVED");
                userAuditService.log(username, "ONBOARDING_APPROVED",
                    "Client " + clientId + " auto-approved (Screening: " + orchResponse.getScreeningResult() + ", Risk: " + orchResponse.getRiskRating() + ")");
            } else if ("ON_HOLD".equalsIgnoreCase(orchResponse.getKycStatus())) {
                // Screening hit OR high risk: create case for manual review
                String reason = "Screening Hit / High Risk - Manual Review Required";
                if ("Hit".equalsIgnoreCase(orchResponse.getScreeningResult())) {
                    reason = "Screening Hit (" + String.join(", ", orchResponse.getHitContext()) + ") - Manual Review Required";
                }
                Long caseId = caseService.createCase(clientId, reason, "SYSTEM");
                clientRepository.updateClientStatus(clientId, "IN_REVIEW");
                userAuditService.log(username, "ONBOARDING_CASE_CREATED",
                    "Case " + caseId + " created for client: " + clientId + " - " + reason);

                // Step 4: Upload documents to Case (if provided)
                if (documents != null && !documents.isEmpty()) {
                    for (int i = 0; i < documents.size(); i++) {
                        MultipartFile file = documents.get(i);
                        String docType = (documentTypes != null && documentTypes.size() > i) ? documentTypes.get(i) : "ONBOARDING";
                        String comment = (documentComments != null && documentComments.size() > i) ? documentComments.get(i) : "Initial Onboarding Document";
                        try {
                            documentService.uploadDocument(caseId, file, docType, comment, username, file.getOriginalFilename());
                        } catch(Exception e) {
                            logger.error("Failed to upload document: {}", file.getOriginalFilename(), e);
                        }
                    }
                }
            }

            Client savedClient = clientRepository.findById(clientId).orElseThrow();
            return ResponseEntity.ok(savedClient);

        } catch (Exception e) {
            logger.error("Error onboarding prospect", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Builds KYC orchestration request from client details.
     * Maps viewer Client entity to KycPrecheckRequest for orchestration service.
     *
     * Required fields for orchestration service:
     * - uniqueClientID, firstName, lastName, businessLine
     * - primaryCitizenship
     * - cityOfBirth (birth place)
     * - countryOfResidence (tax residency country)
     * - residentialAddress.zip (postal code)
     */
    private KycOrchestrationClient.KycPrecheckRequest buildOrchestrationRequest(
            Long clientId, Client client) {

        // Build residential address - REQUIRED: must have zip code
        KycOrchestrationClient.ResidentialAddress resAddr = null;
        if (client.addresses() != null && !client.addresses().isEmpty()) {
            com.venus.kyc.viewer.Address addr = client.addresses().get(0);
            resAddr = new KycOrchestrationClient.ResidentialAddress();
            if (addr.addressLine1() != null) resAddr.setAddressLine1(addr.addressLine1());
            if (addr.addressLine2() != null) resAddr.setAddressLine2(addr.addressLine2());
            if (addr.city() != null) resAddr.setCity(addr.city());
            if (addr.zip() != null) {
                resAddr.setZip(addr.zip());
            } else {
                // Fallback: use city as zip if not provided
                resAddr.setZip(addr.city() != null ? addr.city() : "00000");
            }
        } else {
            // Create minimal address with fallback zip
            resAddr = new KycOrchestrationClient.ResidentialAddress();
            resAddr.setZip("00000");
        }

        KycOrchestrationClient.KycPrecheckRequest request = new KycOrchestrationClient.KycPrecheckRequest()
            .setUniqueClientID("CLIENT-" + clientId)
            .setFirstName(client.firstName())
            .setLastName(client.lastName())
            .setBusinessLine("EIS")  // Default to EIS; can be parameterized if needed
            .setPrimaryCitizenship(client.citizenship1())
            .setSecondCitizenship(client.citizenship2())
            .setResidentialAddress(resAddr);

        // Required fields with fallbacks
        if (client.cityOfBirth() != null && !client.cityOfBirth().isBlank()) {
            request.setCityOfBirth(client.cityOfBirth());
        } else {
            // Fallback to address city or default
            request.setCityOfBirth(client.addresses() != null && !client.addresses().isEmpty()
                ? client.addresses().get(0).city() : "Unknown");
        }

        if (client.countryOfTax() != null && !client.countryOfTax().isBlank()) {
            request.setCountryOfResidence(client.countryOfTax());
        } else {
            // Fallback to primary citizenship or country of residence
            request.setCountryOfResidence(client.citizenship1() != null ? client.citizenship1() : "Unknown");
        }

        // Optional fields
        if (client.dateOfBirth() != null) {
            request.setDob(client.dateOfBirth().toString());
        }
        if (client.countryOfBirth() != null && !client.countryOfBirth().isBlank()) {
            request.setCountryOfBirth(client.countryOfBirth());
        }
        if (client.occupation() != null && !client.occupation().isBlank()) {
            request.setOccupation(client.occupation());
        }

        return request;
    }

    /**
     * Convert risk rating to numerical score for storage.
     * Maps semantic risk levels to numerical scores.
     */
    private int convertRiskRatingToScore(String riskRating) {
        if (riskRating == null) return 0;
        return switch (riskRating.toUpperCase()) {
            case "LOW" -> 25;
            case "MEDIUM" -> 50;
            case "HIGH" -> 75;
            case "UNKNOWN" -> 0;
            default -> 0;
        };
    }
}
