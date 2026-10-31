package com.brightloop.billing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BrightloopBillingApplication {
    public static void main(String[] args) {
        SpringApplication.run(BrightloopBillingApplication.class, args);
    }
}
