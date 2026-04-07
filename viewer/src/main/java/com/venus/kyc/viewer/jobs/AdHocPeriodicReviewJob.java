package com.venus.kyc.viewer.jobs;

import com.venus.kyc.viewer.CaseService;
import org.jobrunr.jobs.annotations.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * One-off job that creates a "Periodic Review" KYC case for each specified client.
 * Enqueued via POST /api/internal/scheduler/jobs/enqueue-periodic-review.
 */
@Component
public class AdHocPeriodicReviewJob {

    private static final Logger log = LoggerFactory.getLogger(AdHocPeriodicReviewJob.class);

    private final CaseService caseService;

    public AdHocPeriodicReviewJob(CaseService caseService) {
        this.caseService = caseService;
    }

    @Job(name = "Ad-Hoc Periodic Review Cases")
    public void execute(List<Long> clientIds, String createdBy) {
        log.info("[AD-HOC REVIEW] Periodic review case creation started for {} client(s), createdBy={}",
                clientIds.size(), createdBy);

        String initiator = (createdBy != null && !createdBy.isBlank()) ? createdBy : "SYSTEM";
        int created = 0;

        for (Long clientId : clientIds) {
            try {
                Long caseId = caseService.createCase(clientId, "Periodic Review", initiator);
                log.info("[AD-HOC REVIEW] Created case {} for client {}", caseId, clientId);
                created++;
            } catch (Exception e) {
                log.error("[AD-HOC REVIEW] Failed to create case for client {}: {}", clientId, e.getMessage(), e);
                throw new RuntimeException(
                        "Ad-Hoc Periodic Review failed for client " + clientId + ": " + e.getMessage(), e);
            }
        }

        log.info("[AD-HOC REVIEW] Completed — {} case(s) created", created);
    }
}
