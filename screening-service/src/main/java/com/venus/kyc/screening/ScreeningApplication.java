package com.venus.kyc.screening;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import com.venus.kyc.screening.nrts.NrtsConfig;

import java.util.concurrent.Executor;

@SpringBootApplication
@EnableScheduling
@EnableAsync
@EnableConfigurationProperties(NrtsConfig.class)
public class ScreeningApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScreeningApplication.class, args);
    }

    /**
     * Dedicated thread pool for mass-screening CSV processing.
     * Each concurrent file drop runs in its own thread.
     * CorePoolSize=2 allows two concurrent 700K runs without overwhelming the JVM.
     */
    @Bean(name = "massScreeningExecutor")
    public Executor massScreeningExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("mass-screen-");
        executor.setKeepAliveSeconds(60);
        executor.initialize();
        return executor;
    }
}
