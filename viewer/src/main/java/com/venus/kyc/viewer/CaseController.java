package com.venus.kyc.viewer;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cases")
public class CaseController {

    private final CaseRepository caseRepository;
    private final CaseService caseService;
    private final EventService eventService;
    private final UserAuditService userAuditService;

    public CaseController(CaseRepository caseRepository, CaseService caseService,
            EventService eventService, UserAuditService userAuditService) {
        this.caseRepository = caseRepository;
        this.caseService = caseService;
        this.eventService = eventService;
        this.userAuditService = userAuditService;
    }

    private String getUserRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("USER");
    }

    @GetMapping
    public List<Case> getCases() {
        return caseRepository.findAll();
    }

    @GetMapping("/{id}")
    public Case getCase(@PathVariable Long id) {
        return caseRepository.findById(id).orElseThrow();
    }

    @GetMapping("/client/{clientID}")
    public List<Case> getCasesByClient(@PathVariable Long clientID) {
        return caseRepository.findByClientId(clientID);
    }

    @GetMapping("/{id}/comments")
    public List<CaseComment> getComments(@PathVariable Long id) {
        return caseRepository.findCommentsByCaseId(id);
    }

    @GetMapping("/{id}/documents")
    public List<CaseDocument> getDocuments(@PathVariable Long id) {
        return caseRepository.findDocumentsByCaseId(id);
    }

    @PostMapping("/migrate")
    public ResponseEntity<Void> migrateCases(Authentication authentication) {
        try {
            caseService.migrateLegacyCase(1L, 1L, authentication.getName());
            caseService.migrateLegacyCase(2L, 2L, authentication.getName());
        } catch (Exception e) {
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/tasks")
    public List<Map<String, Object>> getMyTasks(Authentication authentication) {
        String role = getUserRole(authentication);
        List<String> groups = List.of(role);
        return caseService.getUserTasks(authentication.getName(), groups);
    }

    @DeleteMapping("/tasks")
    public ResponseEntity<Void> deleteAllTasks() {
        caseService.deleteAllTasks();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/admin/tasks")
    public List<Map<String, Object>> getAllTasks(Authentication authentication) {
        return caseService.getAllTasks();
    }

    @GetMapping("/admin/processes")
    public List<Map<String, Object>> getAllProcesses(Authentication authentication) {
        return caseService.getAllProcessInstances();
    }

    @DeleteMapping("/admin/processes/{id}")
    public ResponseEntity<Void> terminateProcess(@PathVariable String id,
            Authentication authentication) {
        caseService.terminateProcessInstance(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/transition")
    public ResponseEntity<Void> transitionCase(@PathVariable Long id, @RequestBody Map<String, String> request,
            Authentication authentication) {
        String role = getUserRole(authentication);
        boolean isAdmin = "ADMIN".equals(role);

        List<String> groups;
        if (isAdmin) {
            groups = List.of("KYC_ANALYST", "KYC_REVIEWER", "AFC_REVIEWER", "ACO_REVIEWER");
        } else {
            groups = List.of(role);
        }

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
        if (comment != null) {
            caseRepository.addComment(id, authentication.getName(), comment, role);
        }

        try {
            caseService.completeTask(taskId, authentication.getName());
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

    @PostMapping("/{id}/assign")
    public ResponseEntity<Void> assignCase(@PathVariable Long id, @RequestBody Map<String, String> request,
            Authentication authentication) {
        String assignee = request.get("assignee");
        try {
            caseService.assignTask(id, assignee, authentication.getName());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/documents")
    public ResponseEntity<Void> uploadDocument(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam("category") String category,
            @RequestParam("comment") String comment,
            Authentication authentication) throws IOException {
        caseRepository.addDocument(
                id,
                file.getOriginalFilename(),
                category,
                file.getContentType(),
                authentication.getName(),
                comment,
                file.getBytes());

        userAuditService.log(authentication.getName(), "UPLOAD_DOCUMENT",
                "Uploaded " + file.getOriginalFilename() + " to case " + id);
        return ResponseEntity.ok().build();
    }

    @PostMapping
    public ResponseEntity<Long> createCase(@RequestBody Map<String, Object> request,
            Authentication authentication) {
        Long clientID = Long.valueOf(request.get("clientID").toString());
        String reason = (String) request.get("reason");
        String role = getUserRole(authentication);

        // Check for MANAGE_CASES permission
        boolean canManage = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("MANAGE_CASES") || a.getAuthority().equals("ADMIN"));

        if (!canManage) {
            return ResponseEntity.status(403).build();
        }

        // Use new Service to start process
        Long caseId = caseService.createCase(clientID, reason, authentication.getName());
        caseRepository.addComment(caseId, authentication.getName(), "Case created via Workflow: " + reason, role);

        userAuditService.log(authentication.getName(), "CREATE_CASE",
                "Created case " + caseId + " for client " + clientID);
        return ResponseEntity.ok(caseId);
    }

    @GetMapping("/documents/{docId}")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable Long docId) {
        CaseDocument doc = caseRepository.findDocumentById(docId).orElseThrow();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(doc.mimeType()))
                .header("Content-Disposition", "attachment; filename=\"" + doc.documentName() + "\"")
                .body(doc.data());
    }

    @GetMapping("/{id}/events")
    public List<CaseEvent> getCaseEvents(@PathVariable Long id) {
        return eventService.getEventsForCase(id);
    }
}
