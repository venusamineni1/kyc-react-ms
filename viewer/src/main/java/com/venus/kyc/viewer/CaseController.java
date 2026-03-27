package com.venus.kyc.viewer;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cases")
@Tag(name = "Case Management", description = "Endpoints for managing KYC cases including creation, workflow transitions, task management, document handling, and case lifecycle")
public class CaseController {

    private final CaseRepository caseRepository;
    private final CaseService caseService;
    private final EventService eventService;
    private final UserAuditService userAuditService;
    private final DocumentService documentService;

    public CaseController(CaseRepository caseRepository, CaseService caseService,
            EventService eventService, UserAuditService userAuditService, DocumentService documentService) {
        this.caseRepository = caseRepository;
        this.caseService = caseService;
        this.eventService = eventService;
        this.userAuditService = userAuditService;
        this.documentService = documentService;
    }

    private String getUserRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .findFirst()
                .orElse("USER");
    }

    @Operation(summary = "Get all cases", description = "Returns a list of all KYC cases in the system")
    @GetMapping
    public List<Case> getCases() {
        return caseRepository.findAll();
    }

    @Operation(summary = "Get case by ID", description = "Returns detailed information for a specific case and logs the view action")
    @GetMapping("/{id}")
    public Case getCase(@Parameter(description = "Case ID") @PathVariable Long id, Authentication authentication) {
        userAuditService.log(authentication.getName(), "VIEW_CASE", "Viewed Case ID: " + id);
        return caseRepository.findById(id).orElseThrow();
    }

    @Operation(summary = "Get cases by client", description = "Returns all cases associated with a specific client")
    @GetMapping("/client/{clientID}")
    public List<Case> getCasesByClient(@Parameter(description = "Client ID") @PathVariable Long clientID) {
        return caseRepository.findByClientId(clientID);
    }

    @Operation(summary = "Get case comments", description = "Returns all comments and notes associated with a case")
    @GetMapping("/{id}/comments")
    public List<CaseComment> getComments(@Parameter(description = "Case ID") @PathVariable Long id) {
        return caseRepository.findCommentsByCaseId(id);
    }

    @Operation(summary = "Get case documents", description = "Returns all documents attached to a case")
    @GetMapping("/{id}/documents")
    public List<CaseDocument> getDocuments(@Parameter(description = "Case ID") @PathVariable Long id) {
        return documentService.getDocuments(id);
    }

    @Operation(summary = "Get document versions", description = "Returns all versions of a specific document within a case")
    @GetMapping("/{id}/documents/versions")
    public List<CaseDocument> getDocumentVersions(@Parameter(description = "Case ID") @PathVariable Long id,
            @Parameter(description = "Document name") @RequestParam String name) {
        return documentService.getDocumentVersions(id, name);
    }

    @Operation(summary = "Migrate legacy cases", description = "Migrates legacy cases to the CMMN workflow engine")
    @PostMapping("/migrate")
    public ResponseEntity<Void> migrateCases(Authentication authentication) {
        try {
            caseService.migrateLegacyCase(1L, 1L, authentication.getName());
            caseService.migrateLegacyCase(2L, 2L, authentication.getName());
        } catch (Exception e) {
        }
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get my tasks", description = "Returns workflow tasks assigned to or claimable by the current user")
    @GetMapping("/tasks")
    public List<Map<String, Object>> getMyTasks(Authentication authentication) {
        List<String> groups = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.startsWith("ROLE_") ? a.substring(5) : a)
                .toList();
        return caseService.getUserTasks(authentication.getName(), groups);
    }

    @Operation(summary = "Complete a task", description = "Marks a workflow task as completed, advancing the case to the next stage")
    @PostMapping("/tasks/{taskId}/complete")
    public ResponseEntity<Void> completeTask(@Parameter(description = "Task ID") @PathVariable String taskId,
            @RequestBody(required = false) Map<String, String> request,
            Authentication authentication) {
        String action = request != null ? request.get("action") : null;
        caseService.completeTask(taskId, authentication.getName(), action);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Delete all tasks", description = "Removes all workflow tasks from the system (admin use only)")
    @DeleteMapping("/tasks")
    public ResponseEntity<Void> deleteAllTasks() {
        caseService.deleteAllTasks();
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get all tasks (admin)", description = "Returns all workflow tasks across all cases for administrative review")
    @GetMapping("/admin/tasks")
    public List<Map<String, Object>> getAllTasks(Authentication authentication) {
        return caseService.getAllTasks();
    }

    @Operation(summary = "Get all processes (admin)", description = "Returns all active CMMN case instances for administrative review")
    @GetMapping("/admin/processes")
    public List<Map<String, Object>> getAllProcesses(Authentication authentication) {
        return caseService.getAllCaseInstances();
    }

    @Operation(summary = "Terminate a process (admin)", description = "Terminates an active CMMN case instance")
    @DeleteMapping("/admin/processes/{id}")
    public ResponseEntity<Void> terminateProcess(
            @Parameter(description = "Process instance ID") @PathVariable String id,
            Authentication authentication) {
        caseService.terminateProcessInstance(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get case timeline", description = "Returns the workflow timeline events for a case showing state transitions and activities")
    @GetMapping("/{id}/timeline")
    public List<CaseService.TimelineItem> getTimeline(@Parameter(description = "Case ID") @PathVariable Long id) {
        Case c = caseRepository.findById(id).orElseThrow();
        if (c.instanceID() == null || c.instanceID().isEmpty()) {
            return List.of();
        }
        return caseService.getCaseTimeline(c.instanceID());
    }

    @Operation(summary = "Get case tasks", description = "Returns active workflow tasks for a specific case, filtered by current user permissions")
    @GetMapping("/{id}/tasks")
    public List<Map<String, Object>> getCaseTasks(@Parameter(description = "Case ID") @PathVariable Long id, Authentication authentication) {
        List<String> groups = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        return caseService.getTasksForCase(id, authentication.getName(), groups);
    }

    @Operation(summary = "Get available actions", description = "Returns discretionary CMMN actions available for a case")
    @GetMapping("/{id}/actions")
    public List<Map<String, Object>> getAvailableActions(@Parameter(description = "Case ID") @PathVariable Long id) {
        Case c = caseRepository.findById(id).orElseThrow();
        if (c.instanceID() == null || c.instanceID().isEmpty() || !"CMMN".equals(c.workflowType())) {
            return List.of();
        }
        return caseService.getAvailableDiscretionaryActions(c.instanceID());
    }

    @Operation(summary = "Trigger case action", description = "Triggers a discretionary CMMN action on a case such as creating ad-hoc tasks")
    @PostMapping("/{id}/actions/{actionId}")
    public ResponseEntity<Void> triggerAction(@Parameter(description = "Case ID") @PathVariable Long id,
            @Parameter(description = "Action ID") @PathVariable String actionId,
            @RequestBody Map<String, Object> variables, Authentication authentication) {
        Case c = caseRepository.findById(id).orElseThrow();
        if (c.instanceID() == null || c.instanceID().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        caseService.triggerDiscretionaryAction(c.instanceID(), actionId, variables);
        caseRepository.addComment(id, authentication.getName(), "Triggered manual action: " + actionId, "SYSTEM");

        userAuditService.log(authentication.getName(), "CREATE_TASK",
                "Triggered discretionary action: " + actionId + " for case " + id);

        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Transition case", description = "Completes the current workflow task to advance the case to the next stage and adds optional comments")
    @PostMapping("/{id}/transition")
    public ResponseEntity<Void> transitionCase(@Parameter(description = "Case ID") @PathVariable Long id,
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        String role = getUserRole(authentication);

        List<String> groups = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        List<Map<String, Object>> tasks = caseService.getUserTasks(authentication.getName(), groups);

        String taskId = tasks.stream()
                .filter(t -> {
                    Object cId = t.get("caseId");
                    boolean match = cId != null && String.valueOf(id).equals(String.valueOf(cId));
                    return match;
                })
                .map(t -> (String) t.get("taskId"))
                .findFirst()
                .orElse(null);

        if (taskId == null) {
            return ResponseEntity.status(404).build();
        }

        String comment = request.get("comment");
        String action = request.get("action");
        if (comment != null) {
            caseRepository.addComment(id, authentication.getName(), comment, role);
        }

        try {
            caseService.completeTask(taskId, authentication.getName(), action);
            userAuditService.log(authentication.getName(), "TRANSITION_CASE", "Transitioned case " + id);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }

        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Assign case", description = "Assigns or reassigns a case to a specific user")
    @PostMapping("/{id}/assign")
    public ResponseEntity<Void> assignCase(@Parameter(description = "Case ID") @PathVariable Long id,
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        String assignee = request.get("assignee");
        try {
            caseService.assignTask(id, assignee, authentication.getName());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Upload document to case", description = "Uploads a file and attaches it to a specific case with optional category and comments")
    @PostMapping("/{id}/documents")
    public ResponseEntity<String> uploadDocument(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "category", required = false, defaultValue = "General") String category,
            @RequestParam(value = "comment", required = false, defaultValue = "") String comment,
            @RequestParam(value = "documentName", required = false) String documentName,
            Authentication authentication) {
        try {
            documentService.uploadDocument(
                    id,
                    file,
                    category,
                    comment,
                    authentication.getName(),
                    documentName);

            userAuditService.log(authentication.getName(), "UPLOAD_DOCUMENT",
                    "Uploaded " + file.getOriginalFilename() + " to case " + id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Upload failed: " + e.getMessage());
        }
    }

    @Operation(summary = "Create a new case", description = "Creates a new KYC case for a client and starts the CMMN workflow")
    @PostMapping
    public ResponseEntity<Long> createCase(@RequestBody Map<String, Object> request,
            Authentication authentication) {
        Long clientID = Long.valueOf(request.get("clientID").toString());
        String reason = (String) request.get("reason");
        // Enforce CMMN
        boolean useCmmn = true;

        String role = getUserRole(authentication);

        // Check for MANAGE_CASES permission
        boolean canManage = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("MANAGE_CASES") || a.getAuthority().equals("ADMIN"));

        if (!canManage) {
            return ResponseEntity.status(403).build();
        }

        // Use new Service to start process
        Long caseId = caseService.createCase(clientID, reason, authentication.getName());
        String type = "CMMN Case";

        String initialComment = (String) request.get("comment");
        if (initialComment != null && !initialComment.isBlank()) {
            caseRepository.addComment(caseId, authentication.getName(), initialComment, role);
        } else {
            caseRepository.addComment(caseId, authentication.getName(), "Case created via " + type + ": " + reason,
                    role);
        }

        userAuditService.log(authentication.getName(), "CREATE_CASE",
                "Created case " + caseId + " for client " + clientID);
        return ResponseEntity.ok(caseId);
    }

    @Operation(summary = "Download case document", description = "Downloads a document file by its ID")
    @GetMapping("/documents/{docId}")
    public ResponseEntity<byte[]> downloadDocument(@Parameter(description = "Document ID") @PathVariable Long docId) {
        return documentService.downloadDocument(docId);
    }

    @Operation(summary = "Get case events", description = "Returns the event log for a specific case")
    @GetMapping("/{id}/events")
    public List<CaseEvent> getCaseEvents(@Parameter(description = "Case ID") @PathVariable Long id) {
        return eventService.getEventsForCase(id);
    }

    @Operation(summary = "Get all tasks (debug)", description = "Returns all raw workflow tasks for debugging purposes")
    @GetMapping("/debug/all-tasks")
    public List<Map<String, Object>> getAllTasksDebug() {
        return caseService.getAllTasksDebug();
    }
}
