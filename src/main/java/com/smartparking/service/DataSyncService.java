package com.smartparking.service;

import com.smartparking.entity.Booking;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service for managing data synchronization between exit and admin pages
 * with 8-hour in-memory cache (does not survive server restarts)
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
    
    // Self-reference for proxy calls
    @Lazy
    @Autowired
    private DataSyncService self;
    
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
     * Get today's exit data with caching
     */
    @Cacheable(value = "exitData", key = "'today-exits'")
    public Map<String, Object> getTodayExitData() {
        // Fetch fresh data and store through proxy
        return self.storeExitData(fetchFreshExitData());
    }
    
    /**
     * Add a new exit record to the cache
     */
    @CachePut(value = "exitData", key = "'today-exits'")
    public Map<String, Object> addExitRecord(Map<String, Object> exitRecord) {
        Map<String, Object> currentData = self.getTodayExitData();
        
        // Create defensive copy of current data
        Map<String, Object> newData = new HashMap<>(currentData);
        
        // Get exits list and create defensive copy
        List<Map<String, Object>> exits = new ArrayList<>(
            (List<Map<String, Object>>) newData.getOrDefault("exits", new ArrayList<>())
        );
        
        // Add the new exit record
        exits.add(0, exitRecord); // Add to beginning
        
        // Limit to last 50 entries
        if (exits.size() > 50) {
            exits = new ArrayList<>(exits.subList(0, 50));
        }
        
        newData.put("exits", exits);
        newData.put("totalExits", exits.size());
        
        // Recalculate revenue
        double totalRevenue = exits.stream()
            .mapToDouble(exit -> {
                Object fee = exit.get("parkingFee");
                return fee instanceof Number ? ((Number) fee).doubleValue() : 0.0;
            })
            .sum();
        newData.put("totalRevenue", totalRevenue);
        
        // Update timestamp
        newData.put("lastUpdated", LocalDateTime.now());
        
        return newData;
    }
    
    /**
     * Get synchronized data for admin page
     */
    public Map<String, Object> getAdminSyncData() {
        Map<String, Object> data = self.getTodayExitData();
        
        // Create defensive copy to avoid mutating cache
        Map<String, Object> adminData = new HashMap<>(data);
        
        // Add admin-specific fields
        adminData.put("syncStatus", "active");
        adminData.put("lastSync", LocalDateTime.now());
        adminData.put("dataAge", calculateDataAge(adminData));
        
        return adminData;
    }
    
    /**
     * Get synchronized data for exit page
     */
    public Map<String, Object> getExitPageSyncData() {
        Map<String, Object> data = self.getTodayExitData();
        
        // Create defensive copy to avoid mutating cache
        Map<String, Object> exitData = new HashMap<>(data);
        
        // Add exit-page specific fields with configurable hourly rate
        exitData.put("activeBookings", getActiveBookingsCount());
        exitData.put("hourlyRate", 50.0); // Default rate - should be from config
        
        return exitData;
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
                .map(booking -> convertBookingToMap(booking, false))
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
     * Convert Booking entity to Map with PII protection
     */
    private Map<String, Object> convertBookingToMap(Booking booking, boolean includePii) {
        Map<String, Object> bookingMap = new HashMap<>();
        
        bookingMap.put("id", booking.getId());
        bookingMap.put("bookingCode", booking.getBookingCode());
        bookingMap.put("vehicleNumber", booking.getVehicleNumber());
        bookingMap.put("vehicleType", booking.getVehicleType());
        bookingMap.put("slotNumber", booking.getSlotNumber());
        bookingMap.put("entryTime", booking.getEntryTime());
        bookingMap.put("exitTime", booking.getExitTime());
        bookingMap.put("parkingFee", booking.getParkingFee());
        bookingMap.put("duration", calculateDuration(booking.getEntryTime(), booking.getExitTime()));
        
        // PII Protection: Only include PII when explicitly authorized
        if (includePii) {
            // Mask customer name - keep first 2 letters
            String customerName = booking.getCustomerName();
            if (customerName != null && customerName.length() > 2) {
                bookingMap.put("customerName", customerName.substring(0, 2) + "***");
            } else {
                bookingMap.put("customerName", "**");
            }
            
            // Mask phone number - keep last 4 digits only
            String phoneNumber = booking.getPhoneNumber();
            if (phoneNumber != null && phoneNumber.length() >= 4) {
                bookingMap.put("phoneNumber", "***-***-" + phoneNumber.substring(phoneNumber.length() - 4));
            } else {
                bookingMap.put("phoneNumber", "***-***-****");
            }
            
            // Add PII handling metadata
            bookingMap.put("piiMasked", true);
            bookingMap.put("accessLevel", "restricted");
        }
        
        return bookingMap;
    }
    
    /**
     * Calculate parking duration
     */
    private String calculateDuration(LocalDateTime entryTime, LocalDateTime exitTime) {
        if (entryTime != null && exitTime != null) {
            long minutes = ChronoUnit.MINUTES.between(entryTime, exitTime);
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
     * Refresh cache with fresh data
     */
    @CacheEvict(value = "exitData", key = "'today-exits'")
    public void refreshCache() {
        self.storeExitData(fetchFreshExitData());
    }
}
