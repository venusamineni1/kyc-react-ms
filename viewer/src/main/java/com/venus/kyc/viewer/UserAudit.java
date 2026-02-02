package com.venus.kyc.viewer;

import java.time.LocalDateTime;

public record UserAudit(
        Long auditID,
        Long userID,
        String username,
        String action,
        String details,
        String ipAddress,
        LocalDateTime timestamp) {
}
