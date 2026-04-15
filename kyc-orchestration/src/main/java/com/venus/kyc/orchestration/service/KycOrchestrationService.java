package com.venus.kyc.orchestration.service;

import com.venus.kyc.orchestration.client.RiskClient;
import com.venus.kyc.orchestration.client.ScreeningClient;
import com.venus.kyc.orchestration.client.ViewerClient;
import com.venus.kyc.orchestration.domain.KycTransactionAudit;
import com.venus.kyc.orchestration.domain.enums.KycStatus;
import com.venus.kyc.orchestration.domain.enums.RiskRating;
import com.venus.kyc.orchestration.domain.enums.ScreeningStatus;
import com.venus.kyc.orchestration.dto.KycPrecheckRequest;
import com.venus.kyc.orchestration.dto.KycPrecheckResponse;
import com.venus.kyc.orchestration.dto.KycStatusResponse;
import com.venus.kyc.orchestration.dto.KycStatusUpdateRequest;
import com.venus.kyc.orchestration.repository.KycTransactionAuditRepository;
import com.venus.kyc.orchestration.util.PiiMaskingUtil;
import lombok.extern.slf4j.Slf4j;
import org.hashids.Hashids;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
@Slf4j
public class KycOrchestrationService {

    private static final List<KycStatus> VALID_UPDATE_TARGETS =
            List.of(KycStatus.APPROVED, KycStatus.COMPLETED);

    private final ViewerClient viewerClient;
    private final ScreeningClient screeningClient;
    private final RiskClient riskClient;
    private final KycAuditService kycAuditService;
    private final KycTransactionAuditRepository auditRepository;
    private final WebhookNotificationService webhookNotificationService;
    private final Executor kycOrchestrationExecutor;
    private final Hashids hashids;

    public KycOrchestrationService(ViewerClient viewerClient,
                                   ScreeningClient screeningClient,
                                   RiskClient riskClient,
                                   KycAuditService kycAuditService,
                                   KycTransactionAuditRepository auditRepository,
                                   WebhookNotificationService webhookNotificationService,
                                   @Qualifier("kycOrchestrationExecutor") Executor kycOrchestrationExecutor,
                                   Hashids hashids) {
        this.viewerClient = viewerClient;
        this.screeningClient = screeningClient;
        this.riskClient = riskClient;
        this.kycAuditService = kycAuditService;
        this.auditRepository = auditRepository;
        this.webhookNotificationService = webhookNotificationService;
        this.kycOrchestrationExecutor = kycOrchestrationExecutor;
        this.hashids = hashids;
    }

    public KycPrecheckResponse initiatePrecheck(KycPrecheckRequest request) {
        log.info("Starting KYC Precheck for client={} businessLine={}",
                request.getUniqueClientID(), request.getBusinessLine());

        // 1. Build initial audit record (PII fields encrypted at rest via AttributeEncryptor)
        KycTransactionAudit initialAudit = new KycTransactionAudit();
        initialAudit.setUniqueClientID(request.getUniqueClientID());
        initialAudit.setBusinessLine(request.getBusinessLine());
        initialAudit.setWebhookUrl(request.getWebhookUrl());

        // Core PII — encrypted by JPA converter
        initialAudit.setFirstName(request.getFirstName());
        initialAudit.setLastName(request.getLastName());
        initialAudit.setDob(request.getDob());

        // Biographical / citizenship
        initialAudit.setCityOfBirth(request.getCityOfBirth());
        initialAudit.setCountryOfBirth(request.getCountryOfBirth());
        initialAudit.setPrimaryCitizenship(request.getPrimaryCitizenship());
        initialAudit.setSecondCitizenship(request.getSecondCitizenship());

        // Residential address — line1/line2 encrypted, city/zip plain
        if (request.getResidentialAddress() != null) {
            initialAudit.setAddrLine1(request.getResidentialAddress().getAddressLine1());
            initialAudit.setAddrLine2(request.getResidentialAddress().getAddressLine2());
            initialAudit.setAddrCity(request.getResidentialAddress().getCity());
            initialAudit.setAddrZip(request.getResidentialAddress().getZip());
        }

        initialAudit.setCountryOfResidence(request.getCountryOfResidence());
        initialAudit.setOccupation(request.getOccupation());

        // Legitimisation document
        initialAudit.setTypeOfLegitimizationDocument(request.getTypeOfLegitimizationDocument());
        initialAudit.setIssuingAuthority(request.getIssuingAuthority());
        initialAudit.setIdentificationNumber(request.getIdentificationNumber()); // encrypted
        initialAudit.setExpirationDate(request.getExpirationDate());

        // Tax identifier — encrypted
        initialAudit.setGermanTaxID(request.getGermanTaxID());

        initialAudit.setKycStatus(KycStatus.PENDING);

        // Persist synchronously so we have the DB-assigned id to encode into kycId (Option D)
        KycTransactionAudit savedAudit = kycAuditService.saveInitial(initialAudit);
        String kycId = hashids.encode(savedAudit.getId());
        log.debug("Audit record persisted: id={} kycId={}", savedAudit.getId(), kycId);

        // 2. Parallel execution: KYC-NCA ingestion (ViewerClient) + NLS screening
        // ViewerClient — soft-fail: orchestration continues even if NCA ingestion fails
        CompletableFuture<String> viewerFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return viewerClient.onboardUser(request);
            } catch (Exception e) {
                log.warn("KYC-NCA ingestion failed for client={}, continuing: {}",
                        request.getUniqueClientID(), e.getMessage());
                return null;
            }
        }, kycOrchestrationExecutor).exceptionally(ex -> {
            log.warn("KYC-NCA ingestion fallback for client={}: {}",
                    request.getUniqueClientID(), ex.getMessage());
            return null;
        });

        // ScreeningClient (NLS/NRTS) — fired off in parallel
        CompletableFuture<ScreeningClient.ScreeningResult> screeningFuture = CompletableFuture.supplyAsync(
                () -> screeningClient.initiateScreening(request), kycOrchestrationExecutor);

        // 3. Await screening result before risk (sequential dependency per KYC-I-16)
        LocalDateTime screeningStartAt = LocalDateTime.now();
        ScreeningClient.ScreeningResult screeningResult = screeningFuture.join();
        LocalDateTime screeningEndAt = LocalDateTime.now();

        log.info("Screening complete for client={}: hit={}", request.getUniqueClientID(), screeningResult.getHit());

        // 4. Invoke CRRE risk rating immediately after screening (KYC-F-06)
        LocalDateTime riskStartAt = LocalDateTime.now();
        RiskClient.RiskResult riskResult = riskClient.calculateRisk(screeningResult);
        LocalDateTime riskEndAt = LocalDateTime.now();

        log.info("Risk rating complete for client={}: rating={}", request.getUniqueClientID(), riskResult.getRiskRating());

        // 5. Derive orchestration outcome (KYC-F-10)
        boolean onHold = "Hit".equalsIgnoreCase(screeningResult.getHit())
                || "HIGH".equalsIgnoreCase(riskResult.getRiskRating());
        KycStatus orchStatus = onHold ? KycStatus.ON_HOLD : KycStatus.APPROVED;
        ScreeningStatus screeningStatus = "Hit".equalsIgnoreCase(screeningResult.getHit())
                ? ScreeningStatus.HIT : ScreeningStatus.NO_HIT;
        RiskRating riskRating = parseRiskRating(riskResult.getRiskRating());

        log.info("Orchestration outcome for client={}: status={}", request.getUniqueClientID(), orchStatus);

        // Viewer catch-up: zero additional latency if already finished during screening+risk
        String userId = viewerFuture.join();

        // 6. Finalize audit asynchronously — do not block the response
        CompletableFuture.runAsync(() ->
                kycAuditService.finalizeAudit(
                        savedAudit,
                        orchStatus, screeningStatus,
                        screeningResult.getHitContext(), riskRating,
                        userId,
                        screeningResult.getScreeningRequestId(),
                        riskResult.getRiskRequestId(),
                        screeningStartAt, screeningEndAt,
                        riskStartAt, riskEndAt),
                kycOrchestrationExecutor
        ).exceptionally(ex -> {
            log.error("Failed to finalize audit for kycId={}: {}", kycId, ex.getMessage());
            return null;
        });

        log.info("KYC Precheck response dispatched: kycId={} name={} status={}",
                kycId, PiiMaskingUtil.mask(request.getFirstName()), orchStatus);

        return KycPrecheckResponse.builder()
                .kycId(kycId)
                .kycStatus(orchStatus.name())
                .screeningResult(screeningResult.getHit())
                .hitContext(screeningResult.getHitContext())
                .riskRating(riskResult.getRiskRating())
                .userId(userId)
                .screeningRequestId(screeningResult.getScreeningRequestId())
                .riskRequestId(riskResult.getRiskRequestId())
                .screeningStartAt(screeningStartAt)
                .screeningEndAt(screeningEndAt)
                .riskStartAt(riskStartAt)
                .riskEndAt(riskEndAt)
                .build();
    }

    public KycStatusResponse getKycStatus(String kycId) {
        KycTransactionAudit audit = findAuditOrThrow(kycId);
        return toStatusResponse(audit, kycId);
    }

    public KycStatusResponse updateKycStatus(String kycId, KycStatusUpdateRequest updateRequest) {
        KycTransactionAudit audit = findAuditOrThrow(kycId);

        KycStatus newStatus = updateRequest.getKycStatus();

        if (!VALID_UPDATE_TARGETS.contains(newStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid target status '" + newStatus + "'. Allowed transitions: APPROVED, COMPLETED.");
        }
        if (audit.getKycStatus() == KycStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "KYC record " + kycId + " is already COMPLETED and cannot be updated.");
        }

        KycTransactionAudit updated = kycAuditService.updateStatus(audit, newStatus);
        KycStatusResponse response = toStatusResponse(updated, kycId);

        // Fire webhook asynchronously if a URL was registered
        webhookNotificationService.notify(updated.getWebhookUrl(), response);

        return response;
    }

    // ---------------------------------------------------------------------------
    // Private helpers
    // ---------------------------------------------------------------------------

    /**
     * Decodes the opaque Hashids kycId back to the DB primary key and loads the record.
     * Returns 404 for any kycId that is syntactically invalid or not present in the DB.
     */
    private KycTransactionAudit findAuditOrThrow(String kycId) {
        long[] decoded = hashids.decode(kycId);
        if (decoded == null || decoded.length == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "KYC record not found for id: " + kycId);
        }
        return auditRepository.findById(decoded[0])
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "KYC record not found for id: " + kycId));
    }

    private KycStatusResponse toStatusResponse(KycTransactionAudit audit, String kycId) {
        return KycStatusResponse.builder()
                .kycId(kycId)
                .uniqueClientID(audit.getUniqueClientID())
                .businessLine(audit.getBusinessLine())
                .kycStatus(audit.getKycStatus())
                .screeningStatus(audit.getScreeningStatus())
                .screeningContext(audit.getScreeningContext())
                .riskRating(audit.getRiskRating())
                .screeningRequestId(audit.getScreeningRequestId())
                .riskRequestId(audit.getRiskRequestId())
                .createdAt(audit.getCreatedAt())
                .updatedAt(audit.getUpdatedAt())
                .build();
    }

    private RiskRating parseRiskRating(String value) {
        try {
            return RiskRating.valueOf(value.toUpperCase());
        } catch (Exception e) {
            log.warn("Unrecognised risk rating value '{}', defaulting to UNKNOWN", value);
            return RiskRating.UNKNOWN;
        }
    }
}
