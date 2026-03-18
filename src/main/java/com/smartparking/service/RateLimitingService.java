package com.smartparking.service;

import com.smartparking.ratelimit.TokenBucket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Rate limiting service using Token Bucket algorithm
 * Thread-safe implementation with automatic cleanup
 */
@Service
public class RateLimitingService {
    
    private static final Logger logger = LoggerFactory.getLogger(RateLimitingService.class);
    
    // Different buckets for different endpoint types
    private final ConcurrentHashMap<String, TokenBucket> lockBuckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, TokenBucket> bookBuckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, TokenBucket> viewBuckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, TokenBucket> receiptBuckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, TokenBucket> generalBuckets = new ConcurrentHashMap<>();
    
    // Statistics
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong allowedRequests = new AtomicLong(0);
    private final AtomicLong blockedRequests = new AtomicLong(0);
    
    @Value("${rate.limit.lock.requests:5}")
    private int lockRequestsPerMinute;
    
    @Value("${rate.limit.book.requests:3}")
    private int bookRequestsPerMinute;
    
    @Value("${rate.limit.view.requests:30}")
    private int viewRequestsPerMinute;
    
    @Value("${rate.limit.receipt.requests:10}")
    private int receiptRequestsPerMinute;
    
    @Value("${rate.limit.general.requests:60}")
    private int generalRequestsPerMinute;
    
    @Value("${rate.limit.cleanup.interval:300000}")
    private long cleanupIntervalMs;
    
    @Value("${rate.limit.bucket.ttl:3600000}")
    private long bucketTtlMs;
    
    /**
     * Check if lock request is allowed for the IP
     */
    public boolean isLockAllowed(String ipAddress) {
        return checkRateLimit(ipAddress, lockBuckets, 
                lockRequestsPerMinute, 60.0, "LOCK");
    }
    
    /**
     * Check if book request is allowed for the IP
     */
    public boolean isBookAllowed(String ipAddress) {
        return checkRateLimit(ipAddress, bookBuckets, 
                bookRequestsPerMinute, 60.0, "BOOK");
    }
    
    /**
     * Check if view request is allowed for the IP
     */
    public boolean isViewAllowed(String ipAddress) {
        return checkRateLimit(ipAddress, viewBuckets, 
                viewRequestsPerMinute, 60.0, "VIEW");
    }
    
    /**
     * Check if receipt download request is allowed for the IP
     */
    public boolean isReceiptAllowed(String ipAddress) {
        return checkRateLimit(ipAddress, receiptBuckets, 
                receiptRequestsPerMinute, 60.0, "RECEIPT");
    }
    
    /**
     * Check if general request is allowed for the IP
     */
    public boolean isGeneralAllowed(String ipAddress) {
        return checkRateLimit(ipAddress, generalBuckets, 
                generalRequestsPerMinute, 60.0, "GENERAL");
    }
    
    /**
     * Core rate limiting logic with error handling
     */
    private boolean checkRateLimit(String ipAddress, ConcurrentHashMap<String, TokenBucket> buckets,
                                  int requestsPerMinute, double windowSizeMinutes, String endpointType) {
        try {
            totalRequests.incrementAndGet();
            
            // Validate input
            if (ipAddress == null || ipAddress.trim().isEmpty()) {
                logger.warn("Invalid IP address provided for rate limiting");
                blockedRequests.incrementAndGet();
                return false;
            }
            
            // Get or create bucket for this IP
            TokenBucket bucket = buckets.computeIfAbsent(ipAddress, ip -> {
                double refillRate = requestsPerMinute / windowSizeMinutes;
                TokenBucket newBucket = new TokenBucket(requestsPerMinute, refillRate);
                logger.debug("Created new {} bucket for IP {}: capacity={}, rate={}/min", 
                        endpointType, ip, requestsPerMinute, requestsPerMinute);
                return newBucket;
            });
            
            // Try to consume a token
            boolean allowed = bucket.tryConsume();
            
            if (allowed) {
                allowedRequests.incrementAndGet();
                logger.debug("Rate limit check PASSED for {} - IP: {}, tokens: {}", 
                        endpointType, ipAddress, bucket.getAvailableTokens());
            } else {
                blockedRequests.incrementAndGet();
                long timeToNext = bucket.getTimeToNextToken();
                logger.warn("Rate limit EXCEEDED for {} - IP: {}, timeToNext: {}ms", 
                        endpointType, ipAddress, timeToNext);
            }
            
            return allowed;
            
        } catch (Exception e) {
            logger.error("Error in rate limiting for {}: IP={}, error={}", endpointType, ipAddress, e.getMessage(), e);
            // Fail open - allow request if rate limiting fails
            allowedRequests.incrementAndGet();
            return true;
        }
    }
    
    /**
     * Get time until next token is available
     */
    public long getTimeToNextToken(String ipAddress, String endpointType) {
        try {
            ConcurrentHashMap<String, TokenBucket> buckets = getBucketsForEndpoint(endpointType);
            if (buckets != null) {
                TokenBucket bucket = buckets.get(ipAddress);
                if (bucket != null) {
                    return bucket.getTimeToNextToken();
                }
            }
        } catch (Exception e) {
            logger.error("Error getting time to next token: {}", e.getMessage());
        }
        return 60000; // Default: 1 minute
    }
    
    /**
     * Get rate limiting statistics
     */
    public RateLimitStats getStats() {
        return new RateLimitStats(
                totalRequests.get(),
                allowedRequests.get(),
                blockedRequests.get(),
                lockBuckets.size(),
                bookBuckets.size(),
                viewBuckets.size(),
                receiptBuckets.size(),
                generalBuckets.size()
        );
    }
    
    /**
     * Cleanup old buckets to prevent memory leaks
     */
    @Scheduled(fixedRateString = "#{${rate.limit.cleanup.interval:300000}}")
    public void cleanupOldBuckets() {
        try {
            long cutoffTime = System.currentTimeMillis() - bucketTtlMs;
            int totalRemoved = 0;
            
            totalRemoved += cleanupBuckets(lockBuckets, cutoffTime, "LOCK");
            totalRemoved += cleanupBuckets(bookBuckets, cutoffTime, "BOOK");
            totalRemoved += cleanupBuckets(viewBuckets, cutoffTime, "VIEW");
            totalRemoved += cleanupBuckets(receiptBuckets, cutoffTime, "RECEIPT");
            totalRemoved += cleanupBuckets(generalBuckets, cutoffTime, "GENERAL");
            
            if (totalRemoved > 0) {
                logger.info("Rate limiting cleanup: removed {} old buckets", totalRemoved);
            }
            
        } catch (Exception e) {
            logger.error("Error during rate limiting cleanup: {}", e.getMessage(), e);
        }
    }
    
    private int cleanupBuckets(ConcurrentHashMap<String, TokenBucket> buckets, long cutoffTime, String type) {
        int removed = 0;
        for (var entry : buckets.entrySet()) {
            TokenBucket bucket = entry.getValue();
            if (bucket.getLastAccessTime() < cutoffTime) {
                buckets.remove(entry.getKey());
                removed++;
            }
        }
        
        if (removed > 0) {
            logger.debug("Cleaned up {} {} buckets", removed, type);
        }
        
        return removed;
    }
    
    private ConcurrentHashMap<String, TokenBucket> getBucketsForEndpoint(String endpointType) {
        switch (endpointType.toUpperCase()) {
            case "LOCK": return lockBuckets;
            case "BOOK": return bookBuckets;
            case "VIEW": return viewBuckets;
            case "RECEIPT": return receiptBuckets;
            case "GENERAL": return generalBuckets;
            default: return null;
        }
    }
    
    /**
     * Statistics data class
     */
    public static class RateLimitStats {
        private final long totalRequests;
        private final long allowedRequests;
        private final long blockedRequests;
        private final int lockBuckets;
        private final int bookBuckets;
        private final int viewBuckets;
        private final int receiptBuckets;
        private final int generalBuckets;
        
        public RateLimitStats(long totalRequests, long allowedRequests, long blockedRequests,
                             int lockBuckets, int bookBuckets, int viewBuckets, 
                             int receiptBuckets, int generalBuckets) {
            this.totalRequests = totalRequests;
            this.allowedRequests = allowedRequests;
            this.blockedRequests = blockedRequests;
            this.lockBuckets = lockBuckets;
            this.bookBuckets = bookBuckets;
            this.viewBuckets = viewBuckets;
            this.receiptBuckets = receiptBuckets;
            this.generalBuckets = generalBuckets;
        }
        
        // Getters
        public long getTotalRequests() { return totalRequests; }
        public long getAllowedRequests() { return allowedRequests; }
        public long getBlockedRequests() { return blockedRequests; }
        public int getLockBuckets() { return lockBuckets; }
        public int getBookBuckets() { return bookBuckets; }
        public int getViewBuckets() { return viewBuckets; }
        public int getReceiptBuckets() { return receiptBuckets; }
        public int getGeneralBuckets() { return generalBuckets; }
        
        public double getBlockRate() {
            return totalRequests > 0 ? (double) blockedRequests / totalRequests * 100 : 0;
        }
    }
}
