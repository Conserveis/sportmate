package com.sportmate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SportmateApplication {
    public static void main(String[] args) {
        SpringApplication.run(SportmateApplication.class, args);
    }
}