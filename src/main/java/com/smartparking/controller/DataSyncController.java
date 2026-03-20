package com.smartparking.controller;

import com.smartparking.service.DataSyncService;
import com.smartparking.service.ExitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Controller for synchronized data between exit and admin pages
 */
@RestController
@RequestMapping("/api/sync")
@CrossOrigin(origins = "${app.cors.allowed-origins:http://localhost:8081}")
public class DataSyncController {
    
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
            System.err.println("Error getting admin sync data: " + e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to get admin data",
                "message", e.getMessage()
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
            System.err.println("Error getting exit sync data: " + e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to get exit data",
                "message", e.getMessage()
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
            
            // Convert to sync format and store
            Map<String, Object> syncExitData = convertExitDetailsToSyncFormat(exitDetails);
            dataSyncService.addExitRecord(syncExitData);
            
            // Return combined response
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("exitDetails", exitDetails);
            response.put("syncData", dataSyncService.getAdminSyncData());
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("Error processing exit with sync: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to process exit",
                "message", e.getMessage()
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
            System.err.println("Error getting today's exits: " + e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to get today's exits",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Force refresh of synchronized data
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshSyncData() {
        try {
            dataSyncService.refreshCache();
            return ResponseEntity.ok(Map.of(
                "message", "Data refreshed successfully",
                "timestamp", LocalDateTime.now(),
                "serverStartTime", dataSyncService.getServerStartTime()
            ));
        } catch (Exception e) {
            System.err.println("Error refreshing sync data: " + e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to refresh data",
                "message", e.getMessage()
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
            System.err.println("Error getting sync status: " + e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to get sync status",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Convert exit details to sync format
     */
    private Map<String, Object> convertExitDetailsToSyncFormat(Map<String, Object> exitDetails) {
        Map<String, Object> syncData = new java.util.HashMap<>();
        
        // Extract relevant fields
        syncData.put("id", exitDetails.get("id"));
        syncData.put("bookingCode", exitDetails.get("bookingCode"));
        syncData.put("vehicleNumber", exitDetails.get("vehicleNumber"));
        syncData.put("customerName", exitDetails.get("customerName"));
        syncData.put("phoneNumber", exitDetails.get("phoneNumber"));
        syncData.put("vehicleType", exitDetails.get("vehicleType"));
        syncData.put("slotNumber", exitDetails.get("slotNumber"));
        syncData.put("entryTime", exitDetails.get("entryTime"));
        syncData.put("exitTime", exitDetails.get("exitTime"));
        syncData.put("parkingFee", exitDetails.get("parkingFee"));
        syncData.put("duration", exitDetails.get("duration"));
        syncData.put("processedAt", LocalDateTime.now());
        
        return syncData;
    }
}
