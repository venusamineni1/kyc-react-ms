package com.venus.kyc.viewer;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/audits")
@Tag(name = "User Audit", description = "Endpoints for retrieving user activity audit logs")
public class UserAuditController {

    private final UserAuditService service;

    public UserAuditController(UserAuditService service) {
        this.service = service;
    }

    @Operation(summary = "Get all audit logs", description = "Returns the complete user audit trail including login, logout, case actions, and administrative operations")
    @GetMapping
    public ResponseEntity<List<UserAudit>> getAudits() {
        return ResponseEntity.ok(service.getAllAudits());
    }
}
