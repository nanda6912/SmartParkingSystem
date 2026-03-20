package com.smartparking.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Duration;
import java.util.Arrays;

/**
 * Cache configuration for persistent data storage with 8-hour TTL
 */
@Configuration
@EnableCaching
@EnableScheduling
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        
        // Configure Caffeine cache with 8-hour TTL
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofHours(8)) // 8 hours TTL
            .expireAfterAccess(Duration.ofHours(8)) // 8 hours idle TTL
            .maximumSize(1000)
            .recordStats());
        
        cacheManager.setCacheNames(Arrays.asList("exitData"));
        
        return cacheManager;
    }
}
