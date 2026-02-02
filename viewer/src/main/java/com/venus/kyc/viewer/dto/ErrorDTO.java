package com.venus.kyc.viewer.dto;

import java.time.LocalDateTime;

public record ErrorDTO(
        int status,
        String error,
        String message,
        String path,
        LocalDateTime timestamp) {
    public ErrorDTO(int status, String error, String message, String path) {
        this(status, error, message, path, LocalDateTime.now());
    }
}
