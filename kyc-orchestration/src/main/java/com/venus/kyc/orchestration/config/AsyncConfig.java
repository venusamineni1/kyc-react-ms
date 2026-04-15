package com.venus.kyc.orchestration.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Named executor for KYC orchestration async tasks (audit writes, parallel client calls).
     * Sizing: corePoolSize handles typical concurrent requests; maxPoolSize absorbs bursts.
     * The spec requires 20 concurrent screening requests/second (KYC-NF-03).
     */
    @Bean(name = "kycOrchestrationExecutor")
    public Executor kycOrchestrationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(20);
        executor.setMaxPoolSize(40);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("kyc-orch-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
