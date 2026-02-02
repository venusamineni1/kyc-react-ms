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

    public ScreeningService(ClientRepository clientRepository, UserAuditService userAuditService,
            @Value("${screening.service.url}") String screeningServiceUrl, RestClient.Builder restClientBuilder) {
        this.clientRepository = clientRepository;
        this.userAuditService = userAuditService;
        this.screeningServiceUrl = screeningServiceUrl;
        this.restClient = restClientBuilder.build();
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
                client.citizenship1());

        String url = this.screeningServiceUrl + "/initiate";

        try {
            return restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(ScreeningDTOs.InitiateScreeningResponse.class);
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
        String url = this.screeningServiceUrl + "/history/" + clientId;
        try {
            return restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(new org.springframework.core.ParameterizedTypeReference<List<ScreeningLog>>() {
                    });
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
