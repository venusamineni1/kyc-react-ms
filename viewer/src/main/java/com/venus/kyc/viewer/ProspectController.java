package com.venus.kyc.viewer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@RestController
@RequestMapping("/api/prospects")
public class ProspectController {

    private static final Logger logger = LoggerFactory.getLogger(ProspectController.class);

    private final ClientRepository clientRepository;
    private final UserAuditService userAuditService;
    private final com.venus.kyc.viewer.screening.ScreeningService screeningService;
    private final com.venus.kyc.viewer.risk.RiskAssessmentService riskService;
    private final CaseService caseService;
    private final DocumentService documentService;
    private final ObjectMapper objectMapper;

    public ProspectController(ClientRepository clientRepository,
                              UserAuditService userAuditService,
                              com.venus.kyc.viewer.screening.ScreeningService screeningService,
                              com.venus.kyc.viewer.risk.RiskAssessmentService riskService,
                              CaseService caseService,
                              DocumentService documentService) {
        this.clientRepository = clientRepository;
        this.userAuditService = userAuditService;
        this.screeningService = screeningService;
        this.riskService = riskService;
        this.caseService = caseService;
        this.documentService = documentService;
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
            
            // 1. Insert Client with status NEW
            Long clientId = clientRepository.insertClient(incomingClient);
            if (incomingClient.addresses() != null) {
                for (com.venus.kyc.viewer.Address addr : incomingClient.addresses()) {
                    clientRepository.addAddress(clientId, addr);
                }
            }
            userAuditService.log(username, "ONBOARDING_NEW", "Created new prospect with ID: " + clientId);

            // 2. Trigger Screening
            clientRepository.updateClientStatus(clientId, "SCREENING_IN_PROGRESS");
            userAuditService.log(username, "ONBOARDING_SCREENING", "Screening in progress for client: " + clientId);
            var screeningResponse = screeningService.initiateScreening(clientId);
            boolean isHit = screeningResponse.hit();

            // 3. Trigger Risk
            clientRepository.updateClientStatus(clientId, "RISK_EVALUATION_IN_PROGRESS");
            userAuditService.log(username, "ONBOARDING_RISK", "Risk evaluation in progress for client: " + clientId);
            var riskResponse = riskService.evaluateRiskForClient(clientId);
            
            boolean isHighRisk = false;
            if (riskResponse != null && riskResponse.clientRiskRatingResponse() != null && !riskResponse.clientRiskRatingResponse().isEmpty()) {
                var riskItem = riskResponse.clientRiskRatingResponse().get(0);
                if (riskItem.overallRiskAssessment() != null) {
                    isHighRisk = "HIGH".equalsIgnoreCase(riskItem.overallRiskAssessment().overallRiskLevel());
                }
            }

            // 4. Evaluate Decision
            if (isHit || isHighRisk) {
                Long caseId = caseService.createCase(clientId, "New Client Onboarding - High Risk / Screening Hit", "SYSTEM");
                clientRepository.updateClientStatus(clientId, "IN_REVIEW");
                userAuditService.log(username, "ONBOARDING_CASE_CREATED", "Case " + caseId + " created for client: " + clientId + " (Hit: " + isHit + ", High Risk: " + isHighRisk + ")");
                
                // Upload documents to Case
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
            } else {
                clientRepository.updateClientStatus(clientId, "APPROVED");
                userAuditService.log(username, "ONBOARDING_APPROVED", "Client " + clientId + " auto-approved (No Hit, Low/Medium Risk)");
            }

            Client savedClient = clientRepository.findById(clientId).orElseThrow();
            return ResponseEntity.ok(savedClient);

        } catch (Exception e) {
            logger.error("Error onboarding prospect", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
