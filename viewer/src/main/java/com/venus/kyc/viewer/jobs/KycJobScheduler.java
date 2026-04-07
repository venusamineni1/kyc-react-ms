package com.venus.kyc.viewer.jobs;

import org.jobrunr.scheduling.JobScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Registers recurring jobs on application startup.
 * Cron expressions can be overridden via application.properties.
 */
@Component
public class KycJobScheduler {

    private static final Logger log = LoggerFactory.getLogger(KycJobScheduler.class);

    private final JobScheduler jobScheduler;
    private final KycRecurringJobs recurringJobs;

    public KycJobScheduler(JobScheduler jobScheduler, KycRecurringJobs recurringJobs) {
        this.jobScheduler = jobScheduler;
        this.recurringJobs = recurringJobs;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void registerRecurringJobs() {
        log.info("Registering KYC recurring jobs...");

        // Daily at 2 AM — re-score all client risk ratings
        jobScheduler.scheduleRecurrently("client-risk-rescoring",
                "0 2 * * *", recurringJobs::clientRiskRescoring);

        // Daily at 3 AM — refresh screening results
        jobScheduler.scheduleRecurrently("screening-refresh",
                "0 3 * * *", recurringJobs::screeningRefresh);

        // Daily at 7 AM — check for KYC reviews past due
        jobScheduler.scheduleRecurrently("kyc-review-expiry-check",
                "0 7 * * *", recurringJobs::kycReviewExpiryCheck);

        // Daily at 6 AM — notify about expiring documents
        jobScheduler.scheduleRecurrently("document-expiry-notification",
                "0 6 * * *", recurringJobs::documentExpiryNotification);

        // Weekly Sunday at 1 AM — clean up stale cases
        jobScheduler.scheduleRecurrently("stale-case-cleanup",
                "0 1 * * 0", recurringJobs::staleCaseCleanup);

        // Monthly 1st at midnight — archive old audit logs
        jobScheduler.scheduleRecurrently("audit-log-archival",
                "0 0 1 * *", recurringJobs::auditLogArchival);

        // Daily at 11 PM — process pending material changes into screening batch
        jobScheduler.scheduleRecurrently("material-change-screening-batch",
                "0 23 * * *", recurringJobs::materialChangeBatchScreening);

        log.info("All KYC recurring jobs registered.");
    }
}
