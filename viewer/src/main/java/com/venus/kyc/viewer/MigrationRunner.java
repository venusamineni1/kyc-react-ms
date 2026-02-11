package com.venus.kyc.viewer;

import com.venus.kyc.viewer.adhoc.AdHocTaskService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class MigrationRunner implements CommandLineRunner {

    private final CaseService caseService;
    private final AdHocTaskService adHocTaskService;

    public MigrationRunner(CaseService caseService, AdHocTaskService adHocTaskService) {
        this.caseService = caseService;
        this.adHocTaskService = adHocTaskService;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Checking for legacy cases to migrate...");

        try {
            // For the known demo data cases
            caseService.migrateLegacyCase(1L, 1L, "admin");
            caseService.migrateLegacyCase(2L, 2L, "admin");
            caseService.migrateLegacyCase(3L, 106L, "admin");
            caseService.migrateLegacyCase(4L, 107L, "admin");
            caseService.migrateLegacyCase(5L, 108L, "admin");
            caseService.migrateLegacyCase(6L, 109L, "admin");
            caseService.migrateLegacyCase(7L, 110L, "admin");
            System.out.println("Migration triggered for cases 1-7.");
        } catch (Exception e) {
            System.out.println("Migration skipped or failed (non-critical): " + e.getMessage());
        }

        System.out.println("Seeding ad-hoc tasks for demonstration...");
        try {
            // Seed some diverse tasks
            adHocTaskService.createTask("admin", "analyst",
                    "Urgent: Please verify the source of wealth for Global Prime Corp (Client #1004).", 1004L);
            adHocTaskService.createTask("analyst", "reviewer",
                    "Requesting manual override for PEP flag on John Doe. Evidence attached in case file.", 1L);
            adHocTaskService.createTask("reviewer", "analyst",
                    "Please provide more clarity on the UBO structure for SolarTech Energy.", 107L);

            // Seed a task for a new case
            adHocTaskService.createTask("admin", "analyst2",
                    "Follow up on missing GID for Hans Schmidt.", 106L);

            // Seed a responded task
            String t3 = adHocTaskService.createTask("admin", "analyst", "Standard periodic review for Client #101.",
                    101L);
            adHocTaskService.respondTask(t3, "analyst",
                    "Preliminary check completed. All documentation appears valid. Moving to final audit.");

            // Seed a completed task
            String t4 = adHocTaskService.createTask("analyst", "admin", "System configuration query.", null);
            adHocTaskService.respondTask(t4, "admin", "The parameter has been updated in the admin panel.");
            adHocTaskService.completeTask(t4, "analyst");

            System.out.println("Ad-hoc task seeding completed.");
        } catch (Exception e) {
            System.out.println("Ad-hoc seeding failed: " + e.getMessage());
        }
    }
}
