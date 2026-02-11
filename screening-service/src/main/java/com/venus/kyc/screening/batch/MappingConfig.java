package com.venus.kyc.screening.batch;

public record MappingConfig(
    Long mappingID,
    String targetPath,
    String sourceField,
    String defaultValue,
    String transformation
) {}
