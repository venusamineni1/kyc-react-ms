package com.venus.kyc.orchestration.controller;

import com.venus.kyc.orchestration.domain.enums.KycStatus;
import com.venus.kyc.orchestration.dto.KycPrecheckResponse;
import com.venus.kyc.orchestration.dto.KycStatusResponse;
import com.venus.kyc.orchestration.exception.GlobalExceptionHandler;
import com.venus.kyc.orchestration.service.KycOrchestrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(KycOrchestrationController.class)
@Import(GlobalExceptionHandler.class)
class KycOrchestrationControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    KycOrchestrationService kycOrchestrationService;

    private String validRequestBody() {
        return """
                {
                  "uniqueClientID": "CL-001",
                  "firstName": "Jane",
                  "lastName": "Smith",
                  "dob": "1990-01-01",
                  "businessLine": "EIS",
                  "cityOfBirth": "Hamburg",
                  "primaryCitizenship": "DE",
                  "residentialAddress": {
                    "addressLine1": "123 Main Street",
                    "city": "Berlin",
                    "zip": "10115"
                  },
                  "countryOfResidence": "DE"
                }
                """;
    }

    private KycPrecheckResponse buildPrecheckResponse() {
        return KycPrecheckResponse.builder()
                .kycId("x3Yq9mZ1")
                .kycStatus("APPROVED")
                .build();
    }

    private KycStatusResponse buildStatusResponse() {
        return KycStatusResponse.builder()
                .kycId("x3Yq9mZ1")
                .kycStatus(KycStatus.APPROVED)
                .build();
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/kyc/initiate
    // -------------------------------------------------------------------------

    @Test
    void initiateKyc_validRequest_returns200() throws Exception {
        when(kycOrchestrationService.initiatePrecheck(any())).thenReturn(buildPrecheckResponse());

        mockMvc.perform(post("/api/v1/kyc/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kycId").value("x3Yq9mZ1"));
    }

    /** Base valid body minus one field — used to test individual mandatory-field violations */
    private String validBodyWithout(String fieldName) {
        // Build a complete body and strip the named top-level field for simple cases.
        // For nested object removal we use explicit strings.
        return switch (fieldName) {
            case "uniqueClientID" -> validRequestBody().replace("\"uniqueClientID\": \"CL-001\",", "");
            case "firstName"      -> validRequestBody().replace("\"firstName\": \"Jane\",", "");
            case "lastName"       -> validRequestBody().replace("\"lastName\": \"Smith\",", "");
            case "dob"            -> validRequestBody().replace("\"dob\": \"1990-01-01\",", "");
            case "businessLine"   -> validRequestBody().replace("\"businessLine\": \"EIS\",", "");
            case "cityOfBirth"    -> validRequestBody().replace("\"cityOfBirth\": \"Hamburg\",", "");
            case "primaryCitizenship" -> validRequestBody().replace("\"primaryCitizenship\": \"DE\",", "");
            case "residentialAddress" -> validRequestBody().replace(
                    "\"residentialAddress\": {\n    \"addressLine1\": \"123 Main Street\",\n    \"city\": \"Berlin\",\n    \"zip\": \"10115\"\n  },", "");
            case "countryOfResidence" -> validRequestBody().replace("\"countryOfResidence\": \"DE\"", "\"countryOfResidence\": \"\"");
            default -> validRequestBody();
        };
    }

    @Test
    void initiateKyc_missingUniqueClientID_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/kyc/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBodyWithout("uniqueClientID")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    void initiateKyc_missingFirstName_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/kyc/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBodyWithout("firstName")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    void initiateKyc_missingDob_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/kyc/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBodyWithout("dob")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    void initiateKyc_invalidBusinessLine_returns400() throws Exception {
        String body = validRequestBody().replace("\"businessLine\": \"EIS\"", "\"businessLine\": \"INVALID\"");
        mockMvc.perform(post("/api/v1/kyc/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    void initiateKyc_missingBusinessLine_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/kyc/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBodyWithout("businessLine")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    void initiateKyc_missingCityOfBirth_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/kyc/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBodyWithout("cityOfBirth")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    void initiateKyc_missingPrimaryCitizenship_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/kyc/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBodyWithout("primaryCitizenship")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    void initiateKyc_missingResidentialAddress_returns400() throws Exception {
        String body = """
                {
                  "uniqueClientID": "CL-001",
                  "firstName": "Jane",
                  "lastName": "Smith",
                  "dob": "1990-01-01",
                  "businessLine": "EIS",
                  "cityOfBirth": "Hamburg",
                  "primaryCitizenship": "DE",
                  "countryOfResidence": "DE"
                }
                """;
        mockMvc.perform(post("/api/v1/kyc/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    void initiateKyc_missingAddressLine1_returns400() throws Exception {
        String body = """
                {
                  "uniqueClientID": "CL-001",
                  "firstName": "Jane",
                  "lastName": "Smith",
                  "dob": "1990-01-01",
                  "businessLine": "EIS",
                  "cityOfBirth": "Hamburg",
                  "primaryCitizenship": "DE",
                  "residentialAddress": {
                    "city": "Berlin",
                    "zip": "10115"
                  },
                  "countryOfResidence": "DE"
                }
                """;
        mockMvc.perform(post("/api/v1/kyc/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    void initiateKyc_missingCountryOfResidence_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/kyc/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBodyWithout("countryOfResidence")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/kyc/{kycId}/status
    // -------------------------------------------------------------------------

    @Test
    void getKycStatus_found_returns200() throws Exception {
        when(kycOrchestrationService.getKycStatus("x3Yq9mZ1")).thenReturn(buildStatusResponse());

        mockMvc.perform(get("/api/v1/kyc/x3Yq9mZ1/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kycId").value("x3Yq9mZ1"));
    }

    @Test
    void getKycStatus_notFound_returns404() throws Exception {
        when(kycOrchestrationService.getKycStatus("bad-id"))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "KYC record not found for id: bad-id"));

        mockMvc.perform(get("/api/v1/kyc/bad-id/status"))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // PATCH /api/v1/orchestration/{kycId}/status
    // -------------------------------------------------------------------------

    @Test
    void updateKycStatus_valid_returns200() throws Exception {
        when(kycOrchestrationService.updateKycStatus(eq("x3Yq9mZ1"), any())).thenReturn(buildStatusResponse());

        mockMvc.perform(patch("/api/v1/orchestration/x3Yq9mZ1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"kycStatus\":\"APPROVED\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void updateKycStatus_missingKycStatus_returns400() throws Exception {
        mockMvc.perform(patch("/api/v1/orchestration/x3Yq9mZ1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateKycStatus_conflict_returns409() throws Exception {
        when(kycOrchestrationService.updateKycStatus(eq("x3Yq9mZ1"), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "already COMPLETED"));

        mockMvc.perform(patch("/api/v1/orchestration/x3Yq9mZ1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"kycStatus\":\"APPROVED\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void updateKycStatus_invalidTransition_returns400() throws Exception {
        when(kycOrchestrationService.updateKycStatus(eq("x3Yq9mZ1"), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid target status"));

        mockMvc.perform(patch("/api/v1/orchestration/x3Yq9mZ1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"kycStatus\":\"APPROVED\"}"))
                .andExpect(status().isBadRequest());
    }
}
