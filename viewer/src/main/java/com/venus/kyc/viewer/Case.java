package com.venus.kyc.viewer;

import java.time.LocalDateTime;

public record Case(
        Long caseID,
        Long clientID,
        String clientName, // Joined from Clients
        LocalDateTime createdDate,
        String reason,
        String assignedTo,
        String status) {
}
