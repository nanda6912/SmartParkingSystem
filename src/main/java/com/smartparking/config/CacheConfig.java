package com.smartparking.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Arrays;

/**
 * Cache configuration for persistent data storage
 */
@Configuration
@EnableCaching
@EnableScheduling
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        
        // Create cache with 8-hour TTL
        ConcurrentMapCache exitDataCache = new ConcurrentMapCache("exitData");
        
        cacheManager.setCaches(Arrays.asList(
            exitDataCache
        ));
        
        return cacheManager;
    }
}
