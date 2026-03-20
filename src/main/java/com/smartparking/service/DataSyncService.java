package com.smartparking.service;

import com.smartparking.entity.Booking;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service for managing data synchronization between exit and admin pages
 * with 8-hour persistent storage that survives server restarts
 */
@Service
public class DataSyncService {
    
    @Autowired
    private ExitService exitService;
    
    @Autowired(required = false)
    private CacheManager cacheManager;
    
    // In-memory cache for backup when cache manager is not available
    private final Map<String, Object> memoryCache = new ConcurrentHashMap<>();
    
    // Server start time
    private final LocalDateTime serverStartTime = LocalDateTime.now();
    
    /**
     * Store exit data with 8-hour expiration
     */
    @CachePut(value = "exitData", key = "'today-exits'")
    public Map<String, Object> storeExitData(Map<String, Object> exitData) {
        // Add timestamp for tracking
        exitData.put("lastUpdated", LocalDateTime.now());
        exitData.put("serverStartTime", serverStartTime);
        
        // Also store in memory cache as backup
        memoryCache.put("today-exits", exitData);
        
        return exitData;
    }
    
    /**
     * Get today's exit data with 8-hour persistence
     */
    @Cacheable(value = "exitData", key = "'today-exits'")
    public Map<String, Object> getTodayExitData() {
        try {
            // Try to get from cache first
            Map<String, Object> cachedData = (Map<String, Object>) memoryCache.get("today-exits");
            if (cachedData != null && isDataValid(cachedData)) {
                return cachedData;
            }
            
            // If no cached data or expired, fetch fresh data
            Map<String, Object> freshData = fetchFreshExitData();
            return storeExitData(freshData);
            
        } catch (Exception e) {
            System.err.println("Error getting today's exit data: " + e.getMessage());
            return getDefaultExitData();
        }
    }
    
    /**
     * Add a new exit record to the cache
     */
    @CachePut(value = "exitData", key = "'today-exits'")
    public Map<String, Object> addExitRecord(Map<String, Object> exitRecord) {
        Map<String, Object> currentData = getTodayExitData();
        
        // Add the new exit record
        List<Map<String, Object>> exits = (List<Map<String, Object>>) currentData.getOrDefault("exits", new ArrayList<>());
        exits.add(0, exitRecord); // Add to beginning for chronological order
        
        // Keep only last 50 records to prevent memory bloat
        if (exits.size() > 50) {
            exits = exits.subList(0, 50);
        }
        
        currentData.put("exits", exits);
        currentData.put("lastUpdated", LocalDateTime.now());
        currentData.put("totalExits", exits.size());
        
        // Recalculate revenue
        double totalRevenue = exits.stream()
            .mapToDouble(exit -> {
                Object fee = exit.get("parkingFee");
                return fee instanceof Number ? ((Number) fee).doubleValue() : 0.0;
            })
            .sum();
        currentData.put("totalRevenue", totalRevenue);
        
        // Store in both caches
        memoryCache.put("today-exits", currentData);
        
        return currentData;
    }
    
    /**
     * Get synchronized data for admin page
     */
    public Map<String, Object> getAdminSyncData() {
        Map<String, Object> data = getTodayExitData();
        
        // Add admin-specific fields
        data.put("syncStatus", "active");
        data.put("lastSync", LocalDateTime.now());
        data.put("dataAge", calculateDataAge(data));
        
        return data;
    }
    
    /**
     * Get synchronized data for exit page
     */
    public Map<String, Object> getExitPageSyncData() {
        Map<String, Object> data = getTodayExitData();
        
        // Add exit-page specific fields
        data.put("activeBookings", getActiveBookingsCount());
        data.put("hourlyRate", 20.0);
        
        return data;
    }
    
    /**
     * Clear old data (scheduled to run every 8 hours)
     */
    @Scheduled(fixedRate = 8 * 60 * 60 * 1000) // 8 hours
    @CacheEvict(value = "exitData", key = "'today-exits'")
    public void clearOldData() {
        System.out.println("Clearing old exit data cache...");
        memoryCache.clear();
        
        // Store fresh data after clearing
        storeExitData(fetchFreshExitData());
    }
    
    /**
     * Validate if cached data is still valid (within 8 hours)
     */
    private boolean isDataValid(Map<String, Object> data) {
        Object lastUpdatedObj = data.get("lastUpdated");
        if (lastUpdatedObj instanceof LocalDateTime) {
            LocalDateTime lastUpdated = (LocalDateTime) lastUpdatedObj;
            return ChronoUnit.HOURS.between(lastUpdated, LocalDateTime.now()) < 8;
        }
        return false;
    }
    
    /**
     * Fetch fresh exit data from database
     */
    private Map<String, Object> fetchFreshExitData() {
        try {
            List<Booking> allBookings = exitService.getAllBookings();
            LocalDateTime now = LocalDateTime.now();
            
            // Filter today's exits
            List<Map<String, Object>> todayExits = allBookings.stream()
                .filter(booking -> booking.getExitTime() != null)
                .filter(booking -> booking.getExitTime().toLocalDate().equals(now.toLocalDate()))
                .sorted((b1, b2) -> b2.getExitTime().compareTo(b1.getExitTime())) // Most recent first
                .map(this::convertBookingToMap)
                .collect(Collectors.toList());
            
            // Calculate statistics
            double totalRevenue = todayExits.stream()
                .mapToDouble(exit -> {
                    Object fee = exit.get("parkingFee");
                    return fee instanceof Number ? ((Number) fee).doubleValue() : 0.0;
                })
                .sum();
            
            Map<String, Object> data = new HashMap<>();
            data.put("exits", todayExits);
            data.put("totalExits", todayExits.size());
            data.put("totalRevenue", totalRevenue);
            data.put("date", now.toLocalDate().toString());
            data.put("lastUpdated", now);
            data.put("serverStartTime", serverStartTime);
            
            return data;
            
        } catch (Exception e) {
            System.err.println("Error fetching fresh exit data: " + e.getMessage());
            return getDefaultExitData();
        }
    }
    
    /**
     * Convert Booking entity to Map for JSON serialization
     */
    private Map<String, Object> convertBookingToMap(Booking booking) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", booking.getId());
        map.put("bookingCode", booking.getBookingCode());
        map.put("vehicleNumber", booking.getVehicleNumber());
        map.put("customerName", booking.getCustomerName());
        map.put("phoneNumber", booking.getPhoneNumber());
        map.put("vehicleType", booking.getVehicleType());
        map.put("slotNumber", booking.getSlotNumber());
        map.put("entryTime", booking.getEntryTime());
        map.put("exitTime", booking.getExitTime());
        map.put("parkingFee", booking.getParkingFee());
        map.put("isActive", booking.getIsActive());
        map.put("duration", calculateDuration(booking));
        return map;
    }
    
    /**
     * Calculate parking duration
     */
    private String calculateDuration(Booking booking) {
        if (booking.getEntryTime() != null && booking.getExitTime() != null) {
            long minutes = ChronoUnit.MINUTES.between(booking.getEntryTime(), booking.getExitTime());
            long hours = minutes / 60;
            long remainingMinutes = minutes % 60;
            return String.format("%d hours %d minutes", hours, remainingMinutes);
        }
        return "N/A";
    }
    
    /**
     * Get active bookings count
     */
    private int getActiveBookingsCount() {
        try {
            List<Booking> allBookings = exitService.getAllBookings();
            return (int) allBookings.stream()
                .filter(booking -> booking.getIsActive() != null && booking.getIsActive())
                .count();
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * Calculate data age in minutes
     */
    private long calculateDataAge(Map<String, Object> data) {
        Object lastUpdatedObj = data.get("lastUpdated");
        if (lastUpdatedObj instanceof LocalDateTime) {
            return ChronoUnit.MINUTES.between((LocalDateTime) lastUpdatedObj, LocalDateTime.now());
        }
        return 0;
    }
    
    /**
     * Get default exit data when no data is available
     */
    private Map<String, Object> getDefaultExitData() {
        Map<String, Object> data = new HashMap<>();
        data.put("exits", new ArrayList<>());
        data.put("totalExits", 0);
        data.put("totalRevenue", 0.0);
        data.put("date", LocalDateTime.now().toLocalDate().toString());
        data.put("lastUpdated", LocalDateTime.now());
        data.put("serverStartTime", serverStartTime);
        return data;
    }
    
    /**
     * Get server start time
     */
    public LocalDateTime getServerStartTime() {
        return serverStartTime;
    }
    
    /**
     * Force refresh of cached data
     */
    @CacheEvict(value = "exitData", key = "'today-exits'")
    public void refreshCache() {
        memoryCache.clear();
        storeExitData(fetchFreshExitData());
    }
}
