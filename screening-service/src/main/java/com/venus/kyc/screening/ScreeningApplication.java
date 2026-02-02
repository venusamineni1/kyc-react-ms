package com.venus.kyc.screening;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ScreeningApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScreeningApplication.class, args);
    }

}
