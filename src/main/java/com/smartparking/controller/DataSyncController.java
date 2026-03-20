package com.smartparking.controller;

import com.smartparking.service.DataSyncService;
import com.smartparking.service.ExitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Controller for synchronized data between exit and admin pages
 */
@RestController
@RequestMapping("/api/sync")
@CrossOrigin(origins = "${app.cors.allowed-origins:http://localhost:8081}")
public class DataSyncController {
    
    private static final Logger log = LoggerFactory.getLogger(DataSyncController.class);
    
    @Autowired
    private DataSyncService dataSyncService;
    
    @Autowired
    private ExitService exitService;
    
    /**
     * Get synchronized data for admin page
     */
    @GetMapping("/admin/data")
    public ResponseEntity<Map<String, Object>> getAdminData() {
        try {
            Map<String, Object> data = dataSyncService.getAdminSyncData();
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            log.error("Error getting admin sync data", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to get admin data"
            ));
        }
    }
    
    /**
     * Get synchronized data for exit page
     */
    @GetMapping("/exit/data")
    public ResponseEntity<Map<String, Object>> getExitData() {
        try {
            Map<String, Object> data = dataSyncService.getExitPageSyncData();
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            log.error("Error getting exit sync data", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to get exit data"
            ));
        }
    }
    
    /**
     * Process vehicle exit and update synchronized data
     */
    @PostMapping("/exit/process/{bookingId}")
    public ResponseEntity<Map<String, Object>> processExitWithSync(@PathVariable Long bookingId) {
        try {
            // Process the exit using existing service
            Map<String, Object> exitDetails = exitService.processExit(bookingId);
            
            // Try to sync data but don't fail the entire operation if sync fails
            String syncStatus = "success";
            String syncError = null;
            
            try {
                // Convert to sync format and store
                Map<String, Object> syncExitData = convertExitDetailsToSyncFormat(exitDetails);
                dataSyncService.addExitRecord(syncExitData);
            } catch (Exception syncEx) {
                log.error("Sync operation failed for bookingId {}: {}", bookingId, syncEx.getMessage(), syncEx);
                syncStatus = "failed";
                syncError = "Data synchronization failed";
            }
            
            // Return combined response
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("exitDetails", exitDetails);
            response.put("syncStatus", syncStatus);
            response.put("syncError", syncError);
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request for bookingId {}: {}", bookingId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Invalid request",
                "message", "Invalid booking ID or request parameters"
            ));
        } catch (Exception e) {
            log.error("Failed to process exit for bookingId {}", bookingId, e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to process exit"
            ));
        }
    }
    
    /**
     * Get today's exits with full synchronization
     */
    @GetMapping("/exits/today")
    public ResponseEntity<Map<String, Object>> getTodayExits() {
        try {
            Map<String, Object> data = dataSyncService.getTodayExitData();
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            log.error("Error getting today's exits", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to get today's exits"
            ));
        }
    }
    
    /**
     * Force refresh of synchronized data (ADMIN only)
     */
    @PostMapping("/refresh")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> refreshSyncData() {
        try {
            dataSyncService.refreshCache();
            return ResponseEntity.ok(Map.of(
                "message", "Data refreshed successfully",
                "timestamp", LocalDateTime.now(),
                "serverStartTime", dataSyncService.getServerStartTime()
            ));
        } catch (Exception e) {
            log.error("Failed to refresh sync data", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to refresh data"
            ));
        }
    }
    
    /**
     * Get synchronization status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getSyncStatus() {
        try {
            Map<String, Object> data = dataSyncService.getTodayExitData();
            
            Map<String, Object> status = new java.util.HashMap<>();
            status.put("serverStartTime", dataSyncService.getServerStartTime());
            status.put("lastUpdated", data.get("lastUpdated"));
            status.put("totalExits", data.get("totalExits"));
            status.put("totalRevenue", data.get("totalRevenue"));
            status.put("syncStatus", "active");
            status.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            log.error("Error getting sync status", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to get sync status"
            ));
        }
    }
    
    /**
     * Convert exit details to sync format with PII protection
     */
    private Map<String, Object> convertExitDetailsToSyncFormat(Map<String, Object> exitDetails) {
        // Defensive null check
        if (exitDetails == null) {
            log.warn("convertExitDetailsToSyncFormat received null exitDetails");
            return new java.util.HashMap<>();
        }
        
        Map<String, Object> syncData = new java.util.HashMap<>();
        
        // Extract relevant fields with null safety
        syncData.put("id", exitDetails.get("id"));
        syncData.put("bookingCode", exitDetails.get("bookingCode"));
        syncData.put("vehicleNumber", exitDetails.get("vehicleNumber"));
        
        // PII Protection: Mask sensitive data
        Object customerName = exitDetails.get("customerName");
        if (customerName != null && customerName instanceof String) {
            String name = (String) customerName;
            if (name.length() > 2) {
                // Keep first 2 letters and mask the rest
                syncData.put("customerName", name.substring(0, 2) + "***");
            } else {
                syncData.put("customerName", "**");
            }
        } else {
            // Always include customerName for consistent response structure
            syncData.put("customerName", "**");
        }
        
        // PII Protection: Mask phone number completely
        syncData.put("phoneNumber", "***-***-****");
        
        syncData.put("vehicleType", exitDetails.get("vehicleType"));
        syncData.put("slotNumber", exitDetails.get("slotNumber"));
        syncData.put("entryTime", exitDetails.get("entryTime"));
        syncData.put("exitTime", exitDetails.get("exitTime"));
        syncData.put("parkingFee", exitDetails.get("parkingFee"));
        syncData.put("duration", exitDetails.get("duration"));
        syncData.put("processedAt", LocalDateTime.now());
        
        // Add PII handling metadata for downstream ACL enforcement
        syncData.put("piiMasked", true);
        syncData.put("accessLevel", "restricted");
        
        return syncData;
    }
}
