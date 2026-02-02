package com.venus.kyc.viewer;

import java.time.LocalDateTime;

public record CaseEvent(
        Long eventID,
        Long caseID,
        String eventType,
        String eventDescription,
        LocalDateTime eventDate,
        String eventSource) {
}
