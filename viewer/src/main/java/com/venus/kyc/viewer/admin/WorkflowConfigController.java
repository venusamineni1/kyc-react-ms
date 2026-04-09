package com.venus.kyc.viewer.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/workflow")
@Tag(name = "Workflow Configuration", description = "Admin endpoints for reading and updating the CMMN workflow definition")
public class WorkflowConfigController {

    private final WorkflowConfigService workflowConfigService;

    public WorkflowConfigController(WorkflowConfigService workflowConfigService) {
        this.workflowConfigService = workflowConfigService;
    }

    @Operation(summary = "Get current workflow definition",
            description = "Returns the current CMMN workflow configuration including stages and discretionary actions")
    @GetMapping("/definition")
    public ResponseEntity<WorkflowConfig> getDefinition() {
        return ResponseEntity.ok(workflowConfigService.getConfig());
    }

    @Operation(summary = "Deploy updated workflow definition",
            description = "Generates a new CMMN definition from the provided config and deploys it via Flowable. "
                    + "In-flight cases continue with the previous version; new cases use the new definition.")
    @PostMapping("/deploy")
    public ResponseEntity<Void> deploy(@RequestBody WorkflowConfig config) {
        try {
            workflowConfigService.deploy(config);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
