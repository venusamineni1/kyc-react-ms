package com.venus.kyc.screening.nrts;

import com.venus.kyc.screening.ScreeningInteractionRepository;
import com.venus.kyc.screening.ScreeningNrtsInteraction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Records one append-only row in ScreeningNrtsInteractions. Shared by both the real
 * NrtsScreeningProvider and MockScreeningProvider, so dev/test (mock) and production (real
 * NRTS) produce the exact same shape of audit data.
 */
@Component
public class NrtsInteractionRecorder {

    private static final Logger log = LoggerFactory.getLogger(NrtsInteractionRecorder.class);

    private final ScreeningInteractionRepository interactionRepository;

    public NrtsInteractionRecorder(ScreeningInteractionRepository interactionRepository) {
        this.interactionRepository = interactionRepository;
    }

    public void record(Long screeningLogId, Long clientId, String externalRequestId,
                        Long nrtsProcessId, Long nrtsReqId, String interactionType,
                        Integer httpStatus, String requestPayload, String responsePayload,
                        boolean isFinal) {
        try {
            interactionRepository.saveInteraction(new ScreeningNrtsInteraction(
                    null, screeningLogId, clientId, externalRequestId, nrtsProcessId, nrtsReqId,
                    interactionType, httpStatus, requestPayload, responsePayload, isFinal,
                    LocalDateTime.now()));
        } catch (Exception e) {
            // Never let audit-trail persistence break the actual screening flow.
            log.error("Failed to record NRTS interaction ({}): {}", interactionType, e.getMessage(), e);
        }
    }
}
