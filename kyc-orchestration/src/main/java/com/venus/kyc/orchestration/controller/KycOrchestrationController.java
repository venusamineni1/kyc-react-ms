package com.venus.kyc.orchestration.controller;

import com.venus.kyc.orchestration.dto.ErrorResponse;
import com.venus.kyc.orchestration.dto.KycPrecheckRequest;
import com.venus.kyc.orchestration.dto.KycPrecheckResponse;
import com.venus.kyc.orchestration.dto.KycStatusResponse;
import com.venus.kyc.orchestration.dto.KycStatusUpdateRequest;
import com.venus.kyc.orchestration.service.KycOrchestrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "KYC Orchestration", description = "KYC pre-check, status enquiry, and internal callback endpoints")
public class KycOrchestrationController {

    private final KycOrchestrationService kycOrchestrationService;

    // -------------------------------------------------------------------------
    // POST /api/v1/kyc/initiate
    // -------------------------------------------------------------------------

    @Operation(
            summary = "Initiate KYC pre-check",
            description = """
                    Starts a KYC pre-check for a new client. The service runs three downstream calls:

                    1. **KYC-NCA ingestion** and **NLS/NRTS screening** in parallel
                    2. **CRRE risk rating** sequentially after screening completes

                    **Outcome logic:**
                    - `APPROVED` — no screening hit and risk rating is not HIGH
                    - `ON_HOLD` — screening returned a Hit **or** risk rating is HIGH

                    When `ON_HOLD`, retain the `kycId` from the response. Either poll \
                    `GET /api/v1/kyc/{kycId}/status` or await a webhook push (if `webhookUrl` was supplied).
                    """,
            tags = {"KYC Precheck"}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Pre-check completed successfully — inspect `kycStatus` for outcome",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = KycPrecheckResponse.class))),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation failed — one or more required fields missing or invalid",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(
                    responseCode = "502",
                    description = "A downstream service (NLS, CRRE, or KYC-NCA) returned an error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(
                    responseCode = "500",
                    description = "Unexpected internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/api/v1/kyc/initiate")
    public ResponseEntity<KycPrecheckResponse> initiateKyc(
            @Valid @RequestBody KycPrecheckRequest request) {
        log.info("KYC initiation request for client: {}", request.getUniqueClientID());
        KycPrecheckResponse response = kycOrchestrationService.initiatePrecheck(request);
        return ResponseEntity.ok(response);
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/kyc/{kycId}/status
    // -------------------------------------------------------------------------

    @Operation(
            summary = "Get KYC status",
            description = """
                    Retrieves the current status of a KYC record by its opaque `kycId`.

                    Use this endpoint to poll for updates when a `webhookUrl` was not provided, \
                    or to confirm the current state at any point in the lifecycle.

                    **Polling guidance:**
                    - `PENDING` — downstream calls still in flight; retry after a short interval
                    - `APPROVED` — clean pass; proceed with onboarding
                    - `ON_HOLD` — KYC review pending; continue polling or await webhook
                    - `COMPLETED` — review finalised; read `screeningStatus` and `riskRating` for outcome

                    Recommended polling interval: **30 seconds**.
                    """,
            tags = {"KYC Status"}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "KYC record found — returns current status",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = KycStatusResponse.class))),
            @ApiResponse(
                    responseCode = "404",
                    description = "No KYC record exists for the supplied `kycId`",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(
                    responseCode = "500",
                    description = "Unexpected internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/api/v1/kyc/{kycId}/status")
    public ResponseEntity<KycStatusResponse> getKycStatus(
            @Parameter(
                    description = "Opaque KYC identifier returned by POST /api/v1/kyc/initiate",
                    required = true,
                    example = "nR7kW2pL")
            @PathVariable String kycId) {
        log.info("KYC status enquiry for kycId: {}", kycId);
        KycStatusResponse response = kycOrchestrationService.getKycStatus(kycId);
        return ResponseEntity.ok(response);
    }

    // -------------------------------------------------------------------------
    // PATCH /api/v1/orchestration/{kycId}/status
    // -------------------------------------------------------------------------

    @Operation(
            summary = "Update KYC status — internal callback",
            description = """
                    **Internal endpoint — intended for KYC-NCA use only. Not part of the EIS/PPR Gateway integration.**

                    Transitions a KYC record to `APPROVED` or `COMPLETED` once KYC-NCA analyst review is complete. \
                    After a successful update the service asynchronously fires a webhook to the EIS/PPR Gateway \
                    if a `webhookUrl` was registered at initiation.

                    **Valid transitions:**
                    - `ON_HOLD` → `APPROVED`
                    - `ON_HOLD` → `COMPLETED`

                    A record already in `COMPLETED` cannot be updated (409 Conflict). \
                    Attempting to set `PENDING` or `ON_HOLD` is rejected (400 Bad Request).
                    """,
            tags = {"Internal Callbacks"}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Status updated — returns the updated KYC record",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = KycStatusResponse.class))),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid target status (`PENDING` or `ON_HOLD` are not valid transitions)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(
                    responseCode = "404",
                    description = "No KYC record exists for the supplied `kycId`",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(
                    responseCode = "409",
                    description = "KYC record is already `COMPLETED` and cannot be updated",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(
                    responseCode = "500",
                    description = "Unexpected internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/api/v1/orchestration/{kycId}/status")
    public ResponseEntity<KycStatusResponse> updateKycStatus(
            @Parameter(
                    description = "Opaque KYC identifier of the record to update",
                    required = true,
                    example = "nR7kW2pL")
            @PathVariable String kycId,
            @Valid @RequestBody KycStatusUpdateRequest updateRequest) {
        log.info("KYC status update for kycId={} newStatus={}", kycId, updateRequest.getKycStatus());
        KycStatusResponse response = kycOrchestrationService.updateKycStatus(kycId, updateRequest);
        return ResponseEntity.ok(response);
    }
}
