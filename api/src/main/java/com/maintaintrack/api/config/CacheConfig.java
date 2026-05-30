package com.maintaintrack.api.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();

        // Each cache gets its own spec via individual registration
        manager.registerCustomCache("dashboard",
                Caffeine.newBuilder()
                        .expireAfterWrite(60, TimeUnit.SECONDS)   // KPIs refreshed every 60s max
                        .maximumSize(10)
                        .build());

        manager.registerCustomCache("equipment",
                Caffeine.newBuilder()
                        .expireAfterWrite(5, TimeUnit.MINUTES)    // Safety net; evicted on writes
                        .maximumSize(500)
                        .build());

        manager.registerCustomCache("parts",
                Caffeine.newBuilder()
                        .expireAfterWrite(5, TimeUnit.MINUTES)    // Safety net; evicted on writes
                        .maximumSize(1000)
                        .build());

        return manager;
    }
}