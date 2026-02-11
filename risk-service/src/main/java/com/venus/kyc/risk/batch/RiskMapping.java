package com.venus.kyc.risk.batch;

public record RiskMapping(
        Long mappingID,
        String targetPath,
        String sourceField,
        String defaultValue,
        String category) {
}
