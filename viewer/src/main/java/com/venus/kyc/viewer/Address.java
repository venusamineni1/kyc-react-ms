package com.venus.kyc.viewer;

public record Address(
                Long addressID,
                String addressType,
                String addressLine1,
                String addressLine2,
                String city,
                String zip,
                String country,
                String addressNumber,
                String addressSupplement) {
}
