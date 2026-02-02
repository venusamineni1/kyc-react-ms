package com.venus.kyc.viewer;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/audits")
public class UserAuditController {

    private final UserAuditService service;

    public UserAuditController(UserAuditService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<UserAudit>> getAudits() {
        return ResponseEntity.ok(service.getAllAudits());
    }
}
