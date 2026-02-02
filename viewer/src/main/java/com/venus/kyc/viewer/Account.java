package com.venus.kyc.viewer;

public record Account(
        Long accountID,
        String accountNumber,
        String accountStatus) {
}
