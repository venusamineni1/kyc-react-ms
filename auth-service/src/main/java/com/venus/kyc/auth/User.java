package com.venus.kyc.auth;

public record User(
        Long userID,
        String username,
        String password,
        String role,
        boolean enabled) {
}
