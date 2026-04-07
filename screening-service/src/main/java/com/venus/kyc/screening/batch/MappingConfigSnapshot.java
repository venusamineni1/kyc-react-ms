package com.venus.kyc.screening.batch;

import java.time.LocalDateTime;

public record MappingConfigSnapshot(
        Long snapshotID,
        String versionLabel,
        LocalDateTime createdAt,
        String createdBy,
        String source,
        String configJson) {
}
