package com.smartparking.filter;

import com.smartparking.service.RateLimitingService;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Rate limiting filter using Token Bucket algorithm
 * Applied to all HTTP requests with different limits per endpoint type
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE) // Execute first
public class RateLimitingFilter implements Filter {
    
    private static final Logger logger = LoggerFactory.getLogger(RateLimitingFilter.class);
    
    @Autowired
    private RateLimitingService rateLimitingService;
    
    // Statistics for this filter
    private final AtomicLong filterRequests = new AtomicLong(0);
    private final AtomicLong filterBlocked = new AtomicLong(0);
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("Rate limiting filter initialized");
    }
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        try {
            filterRequests.incrementAndGet();
            
            // Get client IP address
            String clientIp = getClientIpAddress(httpRequest);
            String requestUri = httpRequest.getRequestURI();
            String method = httpRequest.getMethod();
            
            logger.debug("Rate limiting check: IP={}, URI={}, Method={}", clientIp, requestUri, method);
            
            // Check rate limit based on endpoint type
            boolean allowed = checkRateLimitByEndpoint(clientIp, requestUri, method);
            
            if (!allowed) {
                handleRateLimitExceeded(httpResponse, clientIp, requestUri);
                return;
            }
            
            // Continue with the request
            chain.doFilter(request, response);
            
        } catch (Exception e) {
            logger.error("Error in rate limiting filter: {}", e.getMessage(), e);
            // Fail open - allow request if rate limiting fails
            chain.doFilter(request, response);
        }
    }
    
    /**
     * Check rate limit based on endpoint type
     */
    private boolean checkRateLimitByEndpoint(String clientIp, String requestUri, String method) {
        // Skip rate limiting for health checks and static resources
        if (isExemptEndpoint(requestUri)) {
            logger.debug("Skipping rate limit for exempt endpoint: {}", requestUri);
            return true;
        }
        
        // Determine endpoint type and check corresponding rate limit
        if (requestUri.contains("/parking-slots/lock")) {
            return rateLimitingService.isLockAllowed(clientIp);
        } else if (requestUri.contains("/parking-slots/book")) {
            return rateLimitingService.isBookAllowed(clientIp);
        } else if (requestUri.contains("/receipt/download")) {
            return rateLimitingService.isReceiptAllowed(clientIp);
        } else if (isViewEndpoint(requestUri)) {
            return rateLimitingService.isViewAllowed(clientIp);
        } else {
            return rateLimitingService.isGeneralAllowed(clientIp);
        }
    }
    
    /**
     * Handle rate limit exceeded response
     */
    private void handleRateLimitExceeded(HttpServletResponse response, String clientIp, String requestUri) 
            throws IOException {
        
        filterBlocked.incrementAndGet();
        
        // Get time until next token is available
        long timeToNext = rateLimitingService.getTimeToNextToken(clientIp, getEndpointType(requestUri));
        
        logger.warn("Rate limit exceeded for IP: {}, URI: {}, retry after: {}ms", 
                clientIp, requestUri, timeToNext);
        
        // Set response headers
        response.setStatus(429); // HTTP 429 Too Many Requests
        response.setContentType("application/json");
        response.setHeader("X-RateLimit-Limit", "10");
        response.setHeader("X-RateLimit-Remaining", "0");
        response.setHeader("X-RateLimit-Reset", String.valueOf(System.currentTimeMillis() + timeToNext));
        response.setHeader("Retry-After", String.valueOf((timeToNext + 999) / 1000)); // Round up to seconds
        
        // Return error response
        String errorResponse = String.format(
                "{\"error\":\"Rate limit exceeded\",\"message\":\"Too many requests. Please try again in %d seconds.\",\"retryAfter\":%d}",
                (timeToNext + 999) / 1000,
                (timeToNext + 999) / 1000
        );
        
        response.getWriter().write(errorResponse);
        response.getWriter().flush();
    }
    
    /**
     * Get client IP address considering proxy headers
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            // X-Forwarded-For can contain multiple IPs, take the first one
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    /**
     * Check if endpoint is exempt from rate limiting
     */
    private boolean isExemptEndpoint(String requestUri) {
        return requestUri.equals("/health") ||
               requestUri.equals("/api/health") ||
               requestUri.contains("/test/") ||
               requestUri.contains("/debug/") ||
               requestUri.endsWith(".css") ||
               requestUri.endsWith(".js") ||
               requestUri.endsWith(".png") ||
               requestUri.endsWith(".jpg") ||
               requestUri.endsWith(".gif") ||
               requestUri.endsWith(".ico") ||
               requestUri.contains("/favicon.ico") ||
               requestUri.equals("/api") ||
               requestUri.equals("/api/") ||
               requestUri.equals("/index.html") ||
               requestUri.equals("/api/index.html");
    }
    
    /**
     * Check if endpoint is a view endpoint
     */
    private boolean isViewEndpoint(String requestUri) {
        return requestUri.contains("/parking-slots/floor") ||
               requestUri.contains("/parking-slots/available") ||
               requestUri.equals("/api/parking-slots") ||
               requestUri.matches(".*/parking-slots/\\d+$"); // Get slot by ID
    }
    
    /**
     * Get endpoint type for rate limiting
     */
    private String getEndpointType(String requestUri) {
        if (requestUri.contains("/parking-slots/lock")) {
            return "LOCK";
        } else if (requestUri.contains("/parking-slots/book")) {
            return "BOOK";
        } else if (requestUri.contains("/receipt/download")) {
            return "RECEIPT";
        } else if (isViewEndpoint(requestUri)) {
            return "VIEW";
        } else {
            return "GENERAL";
        }
    }
    
    @Override
    public void destroy() {
        logger.info("Rate limiting filter destroyed. Final stats: requests={}, blocked={}", 
                filterRequests.get(), filterBlocked.get());
    }
    
    /**
     * Get filter statistics
     */
    public FilterStats getStats() {
        return new FilterStats(filterRequests.get(), filterBlocked.get());
    }
    
    /**
     * Filter statistics data class
     */
    public static class FilterStats {
        private final long totalRequests;
        private final long blockedRequests;
        
        public FilterStats(long totalRequests, long blockedRequests) {
            this.totalRequests = totalRequests;
            this.blockedRequests = blockedRequests;
        }
        
        public long getTotalRequests() { return totalRequests; }
        public long getBlockedRequests() { return blockedRequests; }
        
        public double getBlockRate() {
            return totalRequests > 0 ? (double) blockedRequests / totalRequests * 100 : 0;
        }
    }
}
