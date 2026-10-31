package com.brightloop.sms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BrightloopSmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(BrightloopSmsApplication.class, args);
    }
}
