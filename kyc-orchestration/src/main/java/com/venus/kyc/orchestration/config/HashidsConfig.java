package com.venus.kyc.orchestration.config;

import org.hashids.Hashids;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HashidsConfig {

    /**
     * Salt must be kept secret and stable — changing it invalidates all previously issued kycIds.
     * Supply via HASHIDS_SALT environment variable in production.
     */
    @Value("${hashids.salt}")
    private String salt;

    /**
     * Minimum output length. 8 characters gives ~208 trillion combinations at the default alphabet,
     * which is more than sufficient to prevent brute-force enumeration.
     */
    @Value("${hashids.min-length:8}")
    private int minLength;

    @Bean
    public Hashids hashids() {
        return new Hashids(salt, minLength);
    }
}
