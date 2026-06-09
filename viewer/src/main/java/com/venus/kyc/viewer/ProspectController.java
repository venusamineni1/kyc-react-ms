package com.venus.kyc.viewer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.venus.kyc.viewer.client.KycOrchestrationClient;
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
    private final ObjectMapper objectMapper;

    public ProspectController(ClientRepository clientRepository,
                              UserAuditService userAuditService,
                              KycOrchestrationClient kycOrchestrationClient,
                              CaseService caseService,
                              DocumentService documentService) {
        this.clientRepository = clientRepository;
        this.userAuditService = userAuditService;
        this.kycOrchestrationClient = kycOrchestrationClient;
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
     */
    private KycOrchestrationClient.KycPrecheckRequest buildOrchestrationRequest(
            Long clientId, Client client) {

        KycOrchestrationClient.KycPrecheckRequest request = new KycOrchestrationClient.KycPrecheckRequest()
            .setUniqueClientID("CLIENT-" + clientId)
            .setFirstName(client.firstName())
            .setLastName(client.lastName())
            .setBusinessLine("EIS")  // Default to EIS; can be parameterized if needed
            .setPrimaryCitizenship(client.citizenship1())
            .setSecondCitizenship(client.citizenship2());

        // Optional fields
        if (client.dateOfBirth() != null) {
            request.setDob(client.dateOfBirth().toString());
        }
        if (client.cityOfBirth() != null) {
            request.setCityOfBirth(client.cityOfBirth());
        }
        if (client.countryOfBirth() != null) {
            request.setCountryOfBirth(client.countryOfBirth());
        }
        if (client.countryOfTax() != null) {
            request.setCountryOfResidence(client.countryOfTax());
        }
        if (client.occupation() != null) {
            request.setOccupation(client.occupation());
        }

        // Addresses
        if (client.addresses() != null && !client.addresses().isEmpty()) {
            com.venus.kyc.viewer.Address addr = client.addresses().get(0);
            KycOrchestrationClient.ResidentialAddress resAddr = new KycOrchestrationClient.ResidentialAddress();
            if (addr.addressLine1() != null) resAddr.setAddressLine1(addr.addressLine1());
            if (addr.addressLine2() != null) resAddr.setAddressLine2(addr.addressLine2());
            if (addr.city() != null) resAddr.setCity(addr.city());
            if (addr.zip() != null) resAddr.setZip(addr.zip());
            request.setResidentialAddress(resAddr);
        }

        return request;
    }
}
