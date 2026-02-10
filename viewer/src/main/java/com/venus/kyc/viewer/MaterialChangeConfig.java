package com.venus.kyc.viewer;

public record MaterialChangeConfig(
        Long configID,
        String entityName,
        String columnName,
        String category) {
}
