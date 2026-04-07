package com.venus.kyc.viewer.jobs;

import org.jobrunr.jobs.annotations.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Defines the actual job methods that JobRunr will execute.
 * Each method is annotated with @Job so it appears with a readable name in the dashboard.
 */
@Component
public class KycRecurringJobs {

    private static final Logger log = LoggerFactory.getLogger(KycRecurringJobs.class);

    private final MaterialChangeBatchJob materialChangeBatchJob;

    public KycRecurringJobs(MaterialChangeBatchJob materialChangeBatchJob) {
        this.materialChangeBatchJob = materialChangeBatchJob;
    }

    @Job(name = "Client Risk Re-scoring")
    public void clientRiskRescoring() {
        log.info("[JOB] Client Risk Re-scoring started");
        // TODO: call risk-service to re-score all clients whose last assessment is older than threshold
        log.info("[JOB] Client Risk Re-scoring completed");
    }

    @Job(name = "Screening Refresh")
    public void screeningRefresh() {
        log.info("[JOB] Screening Refresh started");
        // TODO: call screening-service to re-run sanctions/PEP checks for active clients
        log.info("[JOB] Screening Refresh completed");
    }

    @Job(name = "KYC Review Expiry Check")
    public void kycReviewExpiryCheck() {
        log.info("[JOB] KYC Review Expiry Check started");
        // TODO: find cases past their review-by date and trigger escalation workflow
        log.info("[JOB] KYC Review Expiry Check completed");
    }

    @Job(name = "Document Expiry Notification")
    public void documentExpiryNotification() {
        log.info("[JOB] Document Expiry Notification started");
        // TODO: scan documents nearing expiry and send notifications
        log.info("[JOB] Document Expiry Notification completed");
    }

    @Job(name = "Stale Case Cleanup")
    public void staleCaseCleanup() {
        log.info("[JOB] Stale Case Cleanup started");
        // TODO: archive or flag cases that have been idle beyond configured threshold
        log.info("[JOB] Stale Case Cleanup completed");
    }

    @Job(name = "Audit Log Archival")
    public void auditLogArchival() {
        log.info("[JOB] Audit Log Archival started");
        // TODO: move audit records older than retention period to archive table / cold storage
        log.info("[JOB] Audit Log Archival completed");
    }

    @Job(name = "Material Change Screening Batch")
    public void materialChangeBatchScreening() {
        materialChangeBatchJob.execute();
    }
}
