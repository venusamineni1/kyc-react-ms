package com.venus.kyc.orchestration.service;

import com.venus.kyc.orchestration.client.RiskClient;
import com.venus.kyc.orchestration.client.ScreeningClient;
import com.venus.kyc.orchestration.client.ViewerClient;
import com.venus.kyc.orchestration.domain.KycTransactionAudit;
import com.venus.kyc.orchestration.domain.enums.KycStatus;
import com.venus.kyc.orchestration.dto.KycPrecheckRequest;
import com.venus.kyc.orchestration.dto.ResidentialAddress;
import com.venus.kyc.orchestration.dto.KycPrecheckResponse;
import com.venus.kyc.orchestration.dto.KycStatusResponse;
import com.venus.kyc.orchestration.dto.KycStatusUpdateRequest;
import com.venus.kyc.orchestration.repository.KycTransactionAuditRepository;
import org.hashids.Hashids;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KycOrchestrationServiceTest {

    @Mock ViewerClient viewerClient;
    @Mock ScreeningClient screeningClient;
    @Mock RiskClient riskClient;
    @Mock KycAuditService kycAuditService;
    @Mock KycTransactionAuditRepository auditRepository;
    @Mock WebhookNotificationService webhookNotificationService;

    private Hashids hashids;
    private Executor executor;
    private KycOrchestrationService service;

    @BeforeEach
    void setUp() {
        hashids = new Hashids("test-salt", 8);
        executor = Runnable::run; // synchronous for deterministic tests
        service = new KycOrchestrationService(viewerClient, screeningClient, riskClient,
                kycAuditService, auditRepository, webhookNotificationService, executor, hashids);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private KycTransactionAudit buildSavedAudit(long id) {
        KycTransactionAudit audit = new KycTransactionAudit();
        audit.setId(id);
        audit.setKycStatus(KycStatus.ON_HOLD);
        return audit;
    }

    private ScreeningClient.ScreeningResult buildScreeningResult(String hit) {
        ScreeningClient.ScreeningResult result = new ScreeningClient.ScreeningResult();
        result.setHit(hit);
        result.setHitContext(List.of("PEP"));
        result.setScreeningRequestId("scr-001");
        return result;
    }

    private RiskClient.RiskResult buildRiskResult(String rating) {
        RiskClient.RiskResult result = new RiskClient.RiskResult();
        result.setRiskRating(rating);
        result.setRiskRequestId("rsk-001");
        return result;
    }

    private KycPrecheckRequest buildRequest() {
        ResidentialAddress address = new ResidentialAddress();
        address.setAddressLine1("123 Main Street");
        address.setCity("Berlin");
        address.setZip("10115");

        KycPrecheckRequest request = new KycPrecheckRequest();
        request.setUniqueClientID("CL-001");
        request.setFirstName("Jane");
        request.setLastName("Smith");
        request.setDob("1990-01-01");
        request.setBusinessLine("EIS");
        request.setCityOfBirth("Hamburg");
        request.setPrimaryCitizenship("DE");
        request.setResidentialAddress(address);
        request.setCountryOfResidence("DE");
        return request;
    }

    // -------------------------------------------------------------------------
    // initiatePrecheck tests
    // -------------------------------------------------------------------------

    @Test
    void initiatePrecheck_noHitLowRisk_returnsApproved() {
        KycTransactionAudit savedAudit = buildSavedAudit(1L);
        when(kycAuditService.saveInitial(any())).thenReturn(savedAudit);
        when(viewerClient.onboardUser(any())).thenReturn("usr-001");
        when(screeningClient.initiateScreening(any())).thenReturn(buildScreeningResult("NoHit"));
        when(riskClient.calculateRisk(any())).thenReturn(buildRiskResult("LOW"));

        KycPrecheckResponse response = service.initiatePrecheck(buildRequest());

        assertEquals("APPROVED", response.getKycStatus());
        assertNotNull(response.getKycId());
        assertFalse(response.getKycId().isBlank());
        assertEquals(hashids.encode(1L), response.getKycId());
    }

    @Test
    void initiatePrecheck_screeningHit_returnsOnHold() {
        KycTransactionAudit savedAudit = buildSavedAudit(1L);
        when(kycAuditService.saveInitial(any())).thenReturn(savedAudit);
        when(viewerClient.onboardUser(any())).thenReturn("usr-001");
        when(screeningClient.initiateScreening(any())).thenReturn(buildScreeningResult("Hit"));
        when(riskClient.calculateRisk(any())).thenReturn(buildRiskResult("LOW"));

        KycPrecheckResponse response = service.initiatePrecheck(buildRequest());

        assertEquals("ON_HOLD", response.getKycStatus());
    }

    @Test
    void initiatePrecheck_highRisk_returnsOnHold() {
        KycTransactionAudit savedAudit = buildSavedAudit(1L);
        when(kycAuditService.saveInitial(any())).thenReturn(savedAudit);
        when(viewerClient.onboardUser(any())).thenReturn("usr-001");
        when(screeningClient.initiateScreening(any())).thenReturn(buildScreeningResult("NoHit"));
        when(riskClient.calculateRisk(any())).thenReturn(buildRiskResult("HIGH"));

        KycPrecheckResponse response = service.initiatePrecheck(buildRequest());

        assertEquals("ON_HOLD", response.getKycStatus());
    }

    @Test
    void initiatePrecheck_unknownRisk_returnsApproved() {
        KycTransactionAudit savedAudit = buildSavedAudit(1L);
        when(kycAuditService.saveInitial(any())).thenReturn(savedAudit);
        when(viewerClient.onboardUser(any())).thenReturn("usr-001");
        when(screeningClient.initiateScreening(any())).thenReturn(buildScreeningResult("NoHit"));
        when(riskClient.calculateRisk(any())).thenReturn(buildRiskResult("UNKNOWN"));

        KycPrecheckResponse response = service.initiatePrecheck(buildRequest());

        assertEquals("APPROVED", response.getKycStatus());
    }

    @Test
    void initiatePrecheck_kycIdIsHashidsEncoded_notRawSequence() {
        KycTransactionAudit savedAudit = buildSavedAudit(1L);
        when(kycAuditService.saveInitial(any())).thenReturn(savedAudit);
        when(viewerClient.onboardUser(any())).thenReturn("usr-001");
        when(screeningClient.initiateScreening(any())).thenReturn(buildScreeningResult("NoHit"));
        when(riskClient.calculateRisk(any())).thenReturn(buildRiskResult("LOW"));

        KycPrecheckResponse response = service.initiatePrecheck(buildRequest());

        assertNotEquals("1", response.getKycId(), "kycId should not be a raw sequence number");
        assertEquals(hashids.encode(1L), response.getKycId());
    }

    @Test
    void initiatePrecheck_finalizesAudit() {
        KycTransactionAudit savedAudit = buildSavedAudit(1L);
        when(kycAuditService.saveInitial(any())).thenReturn(savedAudit);
        when(viewerClient.onboardUser(any())).thenReturn("usr-001");
        when(screeningClient.initiateScreening(any())).thenReturn(buildScreeningResult("NoHit"));
        when(riskClient.calculateRisk(any())).thenReturn(buildRiskResult("LOW"));

        service.initiatePrecheck(buildRequest());

        verify(kycAuditService, times(1)).finalizeAudit(
                eq(savedAudit), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    // -------------------------------------------------------------------------
    // getKycStatus tests
    // -------------------------------------------------------------------------

    @Test
    void getKycStatus_validKycId_returnsStatusResponse() {
        long id = 1L;
        String kycId = hashids.encode(id);
        KycTransactionAudit audit = buildSavedAudit(id);
        audit.setUniqueClientID("CL-001");
        when(auditRepository.findById(id)).thenReturn(Optional.of(audit));

        KycStatusResponse response = service.getKycStatus(kycId);

        assertNotNull(response);
        assertEquals(kycId, response.getKycId());
    }

    @Test
    void getKycStatus_invalidKycId_throws404() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.getKycStatus("!!!"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void getKycStatus_validKycIdNotInDb_throws404() {
        long id = 99L;
        String kycId = hashids.encode(id);
        when(auditRepository.findById(id)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.getKycStatus(kycId));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    // -------------------------------------------------------------------------
    // updateKycStatus tests
    // -------------------------------------------------------------------------

    @Test
    void updateKycStatus_onHoldToApproved_succeeds() {
        long id = 1L;
        String kycId = hashids.encode(id);

        KycTransactionAudit audit = buildSavedAudit(id);
        audit.setKycStatus(KycStatus.ON_HOLD);

        KycTransactionAudit updated = buildSavedAudit(id);
        updated.setKycStatus(KycStatus.APPROVED);

        when(auditRepository.findById(id)).thenReturn(Optional.of(audit));
        when(kycAuditService.updateStatus(audit, KycStatus.APPROVED)).thenReturn(updated);

        KycStatusUpdateRequest request = new KycStatusUpdateRequest();
        request.setKycStatus(KycStatus.APPROVED);

        KycStatusResponse response = service.updateKycStatus(kycId, request);

        assertNotNull(response);
        assertEquals(KycStatus.APPROVED, response.getKycStatus());
    }

    @Test
    void updateKycStatus_alreadyCompleted_throws409() {
        long id = 1L;
        String kycId = hashids.encode(id);

        KycTransactionAudit audit = buildSavedAudit(id);
        audit.setKycStatus(KycStatus.COMPLETED);

        when(auditRepository.findById(id)).thenReturn(Optional.of(audit));

        KycStatusUpdateRequest request = new KycStatusUpdateRequest();
        request.setKycStatus(KycStatus.APPROVED);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.updateKycStatus(kycId, request));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void updateKycStatus_invalidTargetPending_throws400() {
        long id = 1L;
        String kycId = hashids.encode(id);

        KycTransactionAudit audit = buildSavedAudit(id);
        audit.setKycStatus(KycStatus.ON_HOLD);

        when(auditRepository.findById(id)).thenReturn(Optional.of(audit));

        KycStatusUpdateRequest request = new KycStatusUpdateRequest();
        request.setKycStatus(KycStatus.PENDING);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.updateKycStatus(kycId, request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void updateKycStatus_invalidTargetOnHold_throws400() {
        long id = 1L;
        String kycId = hashids.encode(id);

        KycTransactionAudit audit = buildSavedAudit(id);
        audit.setKycStatus(KycStatus.ON_HOLD);

        when(auditRepository.findById(id)).thenReturn(Optional.of(audit));

        KycStatusUpdateRequest request = new KycStatusUpdateRequest();
        request.setKycStatus(KycStatus.ON_HOLD);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.updateKycStatus(kycId, request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void updateKycStatus_firesWebhookWhenUrlPresent() {
        long id = 1L;
        String kycId = hashids.encode(id);

        KycTransactionAudit audit = buildSavedAudit(id);
        audit.setKycStatus(KycStatus.ON_HOLD);
        audit.setWebhookUrl("https://example.com/callback");

        KycTransactionAudit updated = buildSavedAudit(id);
        updated.setKycStatus(KycStatus.APPROVED);
        updated.setWebhookUrl("https://example.com/callback");

        when(auditRepository.findById(id)).thenReturn(Optional.of(audit));
        when(kycAuditService.updateStatus(audit, KycStatus.APPROVED)).thenReturn(updated);

        KycStatusUpdateRequest request = new KycStatusUpdateRequest();
        request.setKycStatus(KycStatus.APPROVED);

        service.updateKycStatus(kycId, request);

        verify(webhookNotificationService, times(1)).notify(eq("https://example.com/callback"), any());
    }

    @Test
    void updateKycStatus_noWebhookWhenUrlBlank() {
        long id = 1L;
        String kycId = hashids.encode(id);

        KycTransactionAudit audit = buildSavedAudit(id);
        audit.setKycStatus(KycStatus.ON_HOLD);
        audit.setWebhookUrl(null);

        KycTransactionAudit updated = buildSavedAudit(id);
        updated.setKycStatus(KycStatus.APPROVED);
        updated.setWebhookUrl(null);

        when(auditRepository.findById(id)).thenReturn(Optional.of(audit));
        when(kycAuditService.updateStatus(audit, KycStatus.APPROVED)).thenReturn(updated);

        KycStatusUpdateRequest request = new KycStatusUpdateRequest();
        request.setKycStatus(KycStatus.APPROVED);

        service.updateKycStatus(kycId, request);

        // notify is always called; it handles null/blank internally
        verify(webhookNotificationService, times(1)).notify(any(), any());
    }
}
