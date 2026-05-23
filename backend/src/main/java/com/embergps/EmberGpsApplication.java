package com.embergps;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EmberGpsApplication {

    public static void main(String[] args) {
        SpringApplication.run(EmberGpsApplication.class, args);
    }
}
