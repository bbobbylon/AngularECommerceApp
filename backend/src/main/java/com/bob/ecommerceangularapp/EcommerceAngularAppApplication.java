package com.bob.ecommerceangularapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // Milestone 6 - drives the weekly marketing-email blast (WeeklyAdScheduler).
public class EcommerceAngularAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(EcommerceAngularAppApplication.class, args);
    }

}
