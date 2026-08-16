package com.maintaintrack.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class MaintainTrackApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(MaintainTrackApiApplication.class, args);
    }
}