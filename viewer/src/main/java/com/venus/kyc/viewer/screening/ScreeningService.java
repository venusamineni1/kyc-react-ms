package com.venus.kyc.viewer.screening;

import com.venus.kyc.viewer.Client;
import com.venus.kyc.viewer.ClientRepository;
import com.venus.kyc.viewer.UserAuditService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.http.MediaType;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class ScreeningService {

    private final ClientRepository clientRepository;
    private final UserAuditService userAuditService;
    private final RestClient restClient;
    private final String screeningServiceUrl;
    private final ScreeningRepository screeningRepository;

    public ScreeningService(ClientRepository clientRepository, UserAuditService userAuditService,
            @Value("${screening.service.url}") String screeningServiceUrl,
            @Value("${internal.api.key}") String internalApiKey,
            RestClient.Builder restClientBuilder,
            ScreeningRepository screeningRepository) {
        this.clientRepository = clientRepository;
        this.userAuditService = userAuditService;
        this.screeningServiceUrl = screeningServiceUrl;
        this.screeningRepository = screeningRepository;
        this.restClient = restClientBuilder
                .defaultHeader("X-Internal-Api-Key", internalApiKey)
                .build();
    }

    public ScreeningDTOs.InitiateScreeningResponse initiateScreening(Long clientId) {
        Optional<Client> clientOpt = clientRepository.findById(clientId);
        if (clientOpt.isEmpty()) {
            throw new RuntimeException("Client not found for ID: " + clientId);
        }
        Client client = clientOpt.get();

        // Audit in Monolith
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication().getName();
        userAuditService.log(username, "RUN_SCREENING", "Initiated screening for client " + clientId);

        // Build Payload for Microservice
        ScreeningDTOs.ScreeningInternalRequest request = new ScreeningDTOs.ScreeningInternalRequest(
                clientId,
                client.firstName(),
                client.lastName(),
                client.dateOfBirth() != null ? client.dateOfBirth().toString() : null,
                client.gender(),
                client.citizenship1(),
                client.citizenship2(),  // nationality
                null,                   // countryOfResidence — not yet extracted from addresses
                null,                   // idType
                null,                   // idNumber
                null,                   // riskRating
                null,                   // comment
                null                    // province
        );

        String url = this.screeningServiceUrl + "/initiate";

        try {
            ScreeningDTOs.InitiateScreeningResponse response = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(ScreeningDTOs.InitiateScreeningResponse.class);

            // Save screening log to viewer database with sourceType=MANUAL
            if (response != null) {
                // Save the actual screening result (Hit or No-Hit), not the orchestration status
                String screeningResult = "Hot".equals(response.result()) ? "Hit" : "No-Hit";
                String externalRequestID = response.processId() != null
                    ? String.valueOf(response.processId())
                    : "NO_HIT_" + System.currentTimeMillis();

                try {
                    String contextsJson = response.alertContexts() != null
                        ? String.valueOf(response.alertContexts())
                        : "[]";

                    Long logId = screeningRepository.saveLog(
                        clientId,
                        "{}",  // request payload
                        "{\"result\": \"" + response.result() + "\", \"alertContexts\": " + contextsJson + "}",  // response payload
                        screeningResult,  // Save the actual screening result
                        externalRequestID,
                        "MANUAL"
                    );

                    // Save screening results for each context
                    @SuppressWarnings("unchecked")
                    java.util.List<String> contexts = (java.util.List<String>) (java.util.List<?>) response.alertContexts();
                    if (contexts != null && !contexts.isEmpty()) {
                        for (String context : contexts) {
                            screeningRepository.saveResult(logId, context, "HIT", context + " alert");
                        }
                        // Mark non-alerted contexts as NO_HIT
                        for (String context : java.util.List.of("PEP", "ADM", "INT", "SAN")) {
                            if (!contexts.contains(context)) {
                                screeningRepository.saveResult(logId, context, "NO_HIT", null);
                            }
                        }
                    } else {
                        // No alerts - mark all contexts as NO_HIT
                        for (String context : java.util.List.of("PEP", "ADM", "INT", "SAN")) {
                            screeningRepository.saveResult(logId, context, "NO_HIT", null);
                        }
                    }
                } catch (Exception e) {
                    // Log but don't fail - screening result was still obtained
                    org.slf4j.LoggerFactory.getLogger(this.getClass()).warn("Failed to save screening log: {}", e.getMessage());
                }
            }

            return response;
        } catch (Exception e) {
            throw new RuntimeException("Failed to call Screening Service", e);
        }
    }

    public ScreeningDTOs.ScreeningStatusResponse checkStatus(String requestId) {
        String url = this.screeningServiceUrl + "/status/" + requestId;
        try {
            return restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(ScreeningDTOs.ScreeningStatusResponse.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to check status from Screening Service", e);
        }
    }

    public List<ScreeningLog> getHistory(Long clientId) {
        // Query viewer's local database instead of screening-service
        return screeningRepository.getHistory(clientId);
    }

    // Batch Screening Proxy Methods

    public List<java.util.Map<String, Object>> getBatchMapping() {
        String url = this.screeningServiceUrl + "/batch/mapping";
        try {
            return restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(new org.springframework.core.ParameterizedTypeReference<List<java.util.Map<String, Object>>>() {
                    });
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public void updateBatchMapping(List<java.util.Map<String, Object>> configs) {
        String url = this.screeningServiceUrl + "/batch/mapping";
        try {
            restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(configs)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            throw new RuntimeException("Failed to update batch mapping", e);
        }
    }

    public String generateBatchTestXml(java.util.Map<String, Object> clientData) {
        String url = this.screeningServiceUrl + "/batch/test-generate";
        try {
            return restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(clientData)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate test XML", e);
        }
    }

    public ScreeningLog getLatestScreening(Long clientId) {
        return screeningRepository.getLatestScreeningLog(clientId);
    }

    public List<ScreeningResult> getScreeningResults(Long logId) {
        return screeningRepository.getResultsByLogId(logId);
    }
}
