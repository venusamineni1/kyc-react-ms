package com.venus.kyc.viewer;

import java.time.LocalDateTime;

public record User(
        String username,
        String password, // In a real app, this should be hashed
        String role,
        boolean active,
        LocalDateTime lastLogin) {
}
