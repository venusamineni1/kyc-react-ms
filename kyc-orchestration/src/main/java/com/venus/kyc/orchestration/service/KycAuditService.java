package com.venus.kyc.orchestration.service;

import com.venus.kyc.orchestration.domain.KycTransactionAudit;
import com.venus.kyc.orchestration.domain.enums.KycStatus;
import com.venus.kyc.orchestration.domain.enums.RiskRating;
import com.venus.kyc.orchestration.domain.enums.ScreeningStatus;
import com.venus.kyc.orchestration.repository.KycTransactionAuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Handles all transactional writes to the KYC audit table.
 * Kept in a separate bean so @Transactional proxying works correctly
 * when called from async lambdas in KycOrchestrationService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KycAuditService {

    private final KycTransactionAuditRepository auditRepository;

    @Transactional
    public KycTransactionAudit saveInitial(KycTransactionAudit audit) {
        KycTransactionAudit saved = auditRepository.save(audit);
        log.debug("Initial audit record created: id={}", saved.getId());
        return saved;
    }

    @Transactional
    public void finalizeAudit(KycTransactionAudit audit,
                               KycStatus kycStatus,
                               ScreeningStatus screeningStatus,
                               List<String> screeningContext,
                               RiskRating riskRating,
                               String viewerUserId,
                               String screeningRequestId,
                               String riskRequestId,
                               LocalDateTime screeningStartAt,
                               LocalDateTime screeningEndAt,
                               LocalDateTime riskStartAt,
                               LocalDateTime riskEndAt) {
        audit.setKycStatus(kycStatus);
        audit.setScreeningStatus(screeningStatus);
        audit.setScreeningContext(screeningContext);
        audit.setRiskRating(riskRating);
        audit.setViewerUserId(viewerUserId);
        audit.setScreeningRequestId(screeningRequestId);
        audit.setRiskRequestId(riskRequestId);
        audit.setScreeningStartAt(screeningStartAt);
        audit.setScreeningEndAt(screeningEndAt);
        audit.setRiskStartAt(riskStartAt);
        audit.setRiskEndAt(riskEndAt);
        auditRepository.save(audit);
        log.debug("Audit record finalized: id={}, kycStatus={}, screeningStatus={}, riskRating={}",
                audit.getId(), kycStatus, screeningStatus, riskRating);
    }

    /**
     * Transitions an existing audit record to a new orchestration status.
     * Used by the PATCH /orchestration/{id}/status callback from KYC-NCA.
     * Returns the updated audit record so the caller can fire a webhook.
     */
    @Transactional
    public KycTransactionAudit updateStatus(KycTransactionAudit audit, KycStatus newStatus) {
        audit.setKycStatus(newStatus);
        KycTransactionAudit saved = auditRepository.save(audit);
        log.info("Orchestration status updated: id={} newStatus={}", audit.getId(), newStatus);
        return saved;
    }
}
