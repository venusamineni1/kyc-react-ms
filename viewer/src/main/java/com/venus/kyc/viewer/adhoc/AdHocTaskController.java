package com.venus.kyc.viewer.adhoc;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/adhoc-tasks")
public class AdHocTaskController {

    private final AdHocTaskService adHocTaskService;

    public AdHocTaskController(AdHocTaskService adHocTaskService) {
        this.adHocTaskService = adHocTaskService;
    }

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

    @GetMapping
    public List<Map<String, Object>> getMyTasks(Authentication auth) {
        return adHocTaskService.getMyTasks(auth.getName());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getTask(@PathVariable String id) {
        Map<String, Object> task = adHocTaskService.getTaskDetails(id);
        if (task == null)
            return ResponseEntity.notFound().build();
        return ResponseEntity.ok(task);
    }

    @PostMapping("/{id}/respond")
    public ResponseEntity<Void> respondTask(@PathVariable String id, @RequestBody Map<String, Object> payload,
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

    @PostMapping("/{id}/reassign")
    public ResponseEntity<Void> reassignTask(@PathVariable String id, @RequestBody Map<String, String> payload,
            Authentication auth) {
        String newAssignee = payload.get("assignee");
        adHocTaskService.reassignTask(id, auth.getName(), newAssignee);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<Void> completeTask(@PathVariable String id, Authentication auth) {
        adHocTaskService.completeTask(id, auth.getName());
        return ResponseEntity.ok().build();
    }
}
