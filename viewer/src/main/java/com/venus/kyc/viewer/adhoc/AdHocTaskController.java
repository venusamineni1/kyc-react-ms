package com.venus.kyc.viewer.adhoc;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/adhoc-tasks")
@Tag(name = "Ad-Hoc Tasks", description = "Endpoints for creating, managing, and completing ad-hoc tasks assigned between users")
public class AdHocTaskController {

    private final AdHocTaskService adHocTaskService;

    public AdHocTaskController(AdHocTaskService adHocTaskService) {
        this.adHocTaskService = adHocTaskService;
    }

    @Operation(summary = "Create ad-hoc task", description = "Creates a new ad-hoc task assigned to a specific user with optional client association")
    @PostMapping
    public ResponseEntity<String> createTask(@RequestBody Map<String, Object> payload, Authentication auth) {
        String assignee = (String) payload.get("assignee");
        String requestText = (String) payload.get("requestText");
        Object clientIDObj = payload.get("clientID");
        Long clientID = null;
        if (clientIDObj != null && !clientIDObj.toString().isEmpty()) {
            try {
                clientID = Long.valueOf(clientIDObj.toString());
            } catch (NumberFormatException e) {
                // Ignore invalid format, treat as null
            }
        }

        String taskId = adHocTaskService.createTask(auth.getName(), assignee, requestText, clientID);
        return ResponseEntity.ok(taskId);
    }

    @Operation(summary = "Get my ad-hoc tasks", description = "Returns all ad-hoc tasks assigned to the currently authenticated user")
    @GetMapping
    public List<Map<String, Object>> getMyTasks(Authentication auth) {
        return adHocTaskService.getMyTasks(auth.getName());
    }

    @Operation(summary = "Get task details", description = "Returns detailed information for a specific ad-hoc task")
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getTask(@Parameter(description = "Task ID") @PathVariable String id) {
        Map<String, Object> task = adHocTaskService.getTaskDetails(id);
        if (task == null)
            return ResponseEntity.notFound().build();
        return ResponseEntity.ok(task);
    }

    @Operation(summary = "Respond to task", description = "Submits a response to an ad-hoc task with response text")
    @PostMapping("/{id}/respond")
    public ResponseEntity<Void> respondTask(@Parameter(description = "Task ID") @PathVariable String id,
            @RequestBody Map<String, Object> payload,
            Authentication auth) {
        try {
            Object responseTextObj = payload.get("responseText");
            String responseText = responseTextObj != null ? String.valueOf(responseTextObj) : "";
            System.out.println(" responding to task " + id + " with text: " + responseText);
            adHocTaskService.respondTask(id, auth.getName(), responseText);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(null); // Return 400, but maybe body is hard to pass with Void type?
            // Actually, ResponseEntity<Void> means no body usually?
            // Let's change return type if possible, or just build 400.
            // But wait, the previous code returned build().
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "Reassign task", description = "Reassigns an ad-hoc task to a different user")
    @PostMapping("/{id}/reassign")
    public ResponseEntity<Void> reassignTask(@Parameter(description = "Task ID") @PathVariable String id,
            @RequestBody Map<String, String> payload,
            Authentication auth) {
        String newAssignee = payload.get("assignee");
        adHocTaskService.reassignTask(id, auth.getName(), newAssignee);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Complete task", description = "Marks an ad-hoc task as completed")
    @PostMapping("/{id}/complete")
    public ResponseEntity<Void> completeTask(@Parameter(description = "Task ID") @PathVariable String id,
            Authentication auth) {
        adHocTaskService.completeTask(id, auth.getName());
        return ResponseEntity.ok().build();
    }
}
