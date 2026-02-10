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
            System.out.println("Migration triggered for cases 1 and 2.");
        } catch (Exception e) {
            System.out.println("Migration skipped or failed (non-critical): " + e.getMessage());
        }

        System.out.println("Seeding ad-hoc tasks for demonstration...");
        try {
            // Seed some diverse tasks
            String t1 = adHocTaskService.createTask("admin", "analyst",
                    "Urgent: Please verify the source of wealth for Global Prime Corp (Client #1004).", 1004L);
            String t2 = adHocTaskService.createTask("analyst", "kyc_manager",
                    "Requesting manual override for PEP flag on John Doe. Evidence attached in case file.", null);

            // Seed a responded task
            String t3 = adHocTaskService.createTask("admin", "analyst", "Standard periodic review for Client #1001.",
                    1001L);
            adHocTaskService.respondTask(t3, "analyst",
                    "Preliminary check completed. All documentation appears valid. Moving to final audit.");

            // Seed a completed task
            String t4 = adHocTaskService.createTask("analyst", "admin", "System configuration query.", null);
            adHocTaskService.respondTask(t4, "admin", "The parameter has been updated in the admin panel.");
            adHocTaskService.completeTask(t4, "analyst");

            System.out.println("Ad-hoc task seeding completed.");
        } catch (Exception e) {
            System.out.println("Ad-hoc seeding skipped or failed (non-critical): " + e.getMessage());
        }
    }
}
