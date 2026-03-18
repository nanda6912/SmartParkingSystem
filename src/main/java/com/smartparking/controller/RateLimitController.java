package com.smartparking.controller;

import com.smartparking.filter.RateLimitingFilter;
import com.smartparking.service.RateLimitingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for rate limiting monitoring and statistics
 */
@RestController
@RequestMapping("/admin/rate-limit")
public class RateLimitController {
    
    private static final Logger logger = LoggerFactory.getLogger(RateLimitController.class);
    
    @Autowired
    private RateLimitingService rateLimitingService;
    
    @Autowired
    private RateLimitingFilter rateLimitingFilter;
    
    /**
     * Get rate limiting statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getRateLimitStats() {
        try {
            RateLimitingService.RateLimitStats serviceStats = rateLimitingService.getStats();
            RateLimitingFilter.FilterStats filterStats = rateLimitingFilter.getStats();
            
            Map<String, Object> stats = new HashMap<>();
            
            // Service statistics
            stats.put("service", Map.of(
                "totalRequests", serviceStats.getTotalRequests(),
                "allowedRequests", serviceStats.getAllowedRequests(),
                "blockedRequests", serviceStats.getBlockedRequests(),
                "blockRate", String.format("%.2f%%", serviceStats.getBlockRate()),
                "activeBuckets", Map.of(
                    "lock", serviceStats.getLockBuckets(),
                    "book", serviceStats.getBookBuckets(),
                    "view", serviceStats.getViewBuckets(),
                    "receipt", serviceStats.getReceiptBuckets(),
                    "general", serviceStats.getGeneralBuckets()
                )
            ));
            
            // Filter statistics
            stats.put("filter", Map.of(
                "totalRequests", filterStats.getTotalRequests(),
                "blockedRequests", filterStats.getBlockedRequests(),
                "blockRate", String.format("%.2f%%", filterStats.getBlockRate())
            ));
            
            // System info
            stats.put("system", Map.of(
                "timestamp", System.currentTimeMillis(),
                "status", "active"
            ));
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            logger.error("Error getting rate limit stats: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to get statistics",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Health check for rate limiting system
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        try {
            Map<String, Object> health = new HashMap<>();
            
            // Check if rate limiting is working
            RateLimitingService.RateLimitStats stats = rateLimitingService.getStats();
            boolean isHealthy = stats.getTotalRequests() >= 0; // Basic health check
            
            health.put("status", isHealthy ? "healthy" : "unhealthy");
            health.put("timestamp", System.currentTimeMillis());
            health.put("totalRequests", stats.getTotalRequests());
            health.put("activeBuckets", 
                stats.getLockBuckets() + stats.getBookBuckets() + 
                stats.getViewBuckets() + stats.getReceiptBuckets() + 
                stats.getGeneralBuckets());
            
            return ResponseEntity.ok(health);
            
        } catch (Exception e) {
            logger.error("Rate limiting health check failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "status", "unhealthy",
                "error", e.getMessage(),
                "timestamp", System.currentTimeMillis()
            ));
        }
    }
}
