package com.venus.kyc.viewer;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserAuditService {

    private final UserAuditRepository repository;

    public UserAuditService(UserAuditRepository repository) {
        this.repository = repository;
    }

    public void log(String username, String action, String details) {
        // ideally look up userID from username if needed, or pass null
        // For now, logging username is key.
        repository.logAction(null, username, action, details, null);
    }

    public void log(Long userId, String username, String action, String details) {
        repository.logAction(userId, username, action, details, null);
    }

    public List<UserAudit> getAllAudits() {
        return repository.findAll();
    }
}
