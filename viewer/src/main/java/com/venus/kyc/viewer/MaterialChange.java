package com.venus.kyc.viewer;

import java.time.LocalDateTime;

public record MaterialChange(
        Long changeID,
        LocalDateTime changeDate,
        Long clientID,
        String clientName,
        Long entityID,
        String entityName,
        String columnName,
        String operationType,
        String oldValue,
        String newValue,
        String status,
        String category) {
}
