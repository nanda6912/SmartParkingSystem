package com.smartparking.controller;

import com.smartparking.entity.Booking;
import com.smartparking.service.ExitService;
import com.smartparking.service.DataSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ContentDisposition;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Controller for vehicle exit and fee management
 */
@RestController
@RequestMapping("/api/exit")
public class ExitController {
    
    @Autowired
    private ExitService exitService;
    
    @Autowired
    private DataSyncService dataSyncService;
    
    /**
     * Get all active bookings for exit management
     */
    @GetMapping("/active-bookings")
    public ResponseEntity<List<Map<String, Object>>> getActiveBookings() {
        try {
            List<Map<String, Object>> bookings = exitService.getActiveBookingsForExit();
            return ResponseEntity.ok(bookings);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Calculate fee for a booking
     */
    @GetMapping("/calculate-fee/{bookingId}")
    public ResponseEntity<Map<String, Object>> calculateFee(@PathVariable Long bookingId) {
        try {
            Map<String, Object> feeDetails = exitService.calculateFee(bookingId);
            return ResponseEntity.ok(feeDetails);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to calculate fee",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Process vehicle exit and generate receipt
     */
    @PostMapping("/process/{bookingId}")
    public ResponseEntity<Map<String, Object>> processExit(@PathVariable Long bookingId) {
        try {
            // Process the exit using existing service
            Map<String, Object> exitDetails = exitService.processExit(bookingId);
            
            // Try to sync data but don't fail the entire operation if sync fails
            String syncStatus = "success";
            String syncError = null;
            
            try {
                // Convert to sync format and store for synchronization
                Map<String, Object> syncExitData = convertExitDetailsToSyncFormat(exitDetails);
                dataSyncService.addExitRecord(syncExitData);
            } catch (Exception syncEx) {
                System.err.println("Sync operation failed for bookingId " + bookingId + ": " + syncEx.getMessage());
                syncEx.printStackTrace();
                syncStatus = "failed";
                syncError = "Data synchronization failed";
            }
            
            // Return combined response with sync status
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("exitDetails", exitDetails);
            response.put("syncStatus", syncStatus);
            response.put("syncError", syncError);
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to process exit",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Clean up old bookings with invalid codes
     */
    @PostMapping("/cleanup-old-bookings")
    public ResponseEntity<Map<String, Object>> cleanupOldBookings() {
        try {
            Map<String, Object> result = exitService.cleanupOldBookings();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to cleanup old bookings",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Download exit receipt
     */
    @GetMapping("/receipt/{bookingId}")
    public ResponseEntity<byte[]> downloadExitReceipt(@PathVariable Long bookingId) {
        try {
            // Find booking by ID for exit receipt
            Booking booking = exitService.findBookingById(bookingId)
                    .orElseThrow(() -> new RuntimeException("Booking not found with ID: " + bookingId));
            
            // Generate receipt content (works for both active and exited bookings)
            String receiptContent = exitService.generateExitReceipt(booking);
            
            // Create downloadable file with proper encoding
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
            String filename = "exit_receipt_" + bookingId + "_" + LocalDateTime.now().format(formatter) + ".txt";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            headers.setContentDisposition(ContentDisposition.builder("attachment").filename(filename).build());
            headers.setContentLength(receiptContent.getBytes().length);
            
            return new ResponseEntity<>(receiptContent.getBytes(StandardCharsets.UTF_8), headers, HttpStatus.OK);
            
        } catch (RuntimeException e) {
            // Log the error and return a proper response
            System.err.println("Error generating receipt: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(("Error: " + e.getMessage()).getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            // Log the error and return a proper response
            System.err.println("Unexpected error generating receipt: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Internal error: " + e.getMessage()).getBytes(StandardCharsets.UTF_8));
        }
    }
    
    @GetMapping("/receipt/by-code/{bookingCode}")
    public ResponseEntity<byte[]> downloadExitReceiptByCode(@PathVariable String bookingCode) {
        try {
            // Find booking by code for exit receipt
            List<Booking> bookings = exitService.getAllBookings();
            Booking booking = bookings.stream()
                    .filter(b -> bookingCode.equals(b.getBookingCode()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Booking not found with code: " + bookingCode));
            
            // Generate receipt content (works for both active and exited bookings)
            String receiptContent = exitService.generateExitReceipt(booking);
            
            // Create downloadable file with proper encoding
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
            String filename = "exit_receipt_" + bookingCode + "_" + LocalDateTime.now().format(formatter) + ".txt";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            headers.setContentDisposition(ContentDisposition.builder("attachment").filename(filename).build());
            headers.setContentLength(receiptContent.getBytes().length);
            
            return new ResponseEntity<>(receiptContent.getBytes(StandardCharsets.UTF_8), headers, HttpStatus.OK);
            
        } catch (RuntimeException e) {
            // Log the error and return a proper response
            System.err.println("Error generating receipt by code: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(("Error: " + e.getMessage()).getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            // Log the error and return a proper response
            System.err.println("Unexpected error generating receipt by code: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Internal error: " + e.getMessage()).getBytes(StandardCharsets.UTF_8));
        }
    }
    
    /**
     * Get today's exit statistics for exit page
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getTodayStats() {
        try {
            LocalDate today = LocalDate.now();
            
            // Get all bookings
            List<Booking> allBookings = exitService.getAllBookings();
            
            // Filter today's exits
            List<Booking> todayExits = allBookings.stream()
                    .filter(booking -> booking.getExitTime() != null)
                    .filter(booking -> booking.getExitTime().toLocalDate().equals(today))
                    .collect(Collectors.toList());
            
            // Get active bookings
            List<Booking> activeBookings = allBookings.stream()
                    .filter(booking -> booking.getIsActive() != null && booking.getIsActive())
                    .collect(Collectors.toList());
            
            // Calculate total revenue for today
            double todayRevenue = todayExits.stream()
                    .mapToDouble(booking -> booking.getParkingFee() != null ? booking.getParkingFee() : 0.0)
                    .sum();
            
            // Prepare response
            Map<String, Object> stats = new java.util.HashMap<>();
            stats.put("todayExits", todayExits.size());
            stats.put("todayRevenue", todayRevenue);
            stats.put("activeBookings", activeBookings.size());
            stats.put("hourlyRate", 20.0); // Default hourly rate
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            System.err.println("Error fetching today's stats: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "Failed to fetch today's statistics"));
        }
    }
    
    /**
     * Get today's statistics for admin dashboard
     */
    @GetMapping("/admin/today-stats")
    public ResponseEntity<Map<String, Object>> getTodayStatsForAdmin() {
        try {
            LocalDate today = LocalDate.now();
            
            // Get all bookings
            List<Booking> allBookings = exitService.getAllBookings();
            
            // Filter today's exits
            List<Booking> todayExits = allBookings.stream()
                    .filter(booking -> booking.getExitTime() != null)
                    .filter(booking -> booking.getExitTime().toLocalDate().equals(today))
                    .collect(Collectors.toList());
            
            // Get active bookings
            List<Booking> activeBookings = allBookings.stream()
                    .filter(booking -> booking.getIsActive() != null && booking.getIsActive())
                    .collect(Collectors.toList());
            
            // Calculate total revenue for today
            double todayRevenue = todayExits.stream()
                    .mapToDouble(booking -> booking.getParkingFee() != null ? booking.getParkingFee() : 0.0)
                    .sum();
            
            // Prepare response
            Map<String, Object> stats = new java.util.HashMap<>();
            stats.put("date", today.toString());
            stats.put("totalExits", todayExits.size());
            stats.put("totalRevenue", todayRevenue);
            stats.put("activeBookings", activeBookings.size());
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            System.err.println("Error fetching today's stats: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "Failed to fetch today's statistics"));
        }
    }
    @GetMapping("/debug/vehicle/{vehicleNumber}")
    public ResponseEntity<String> checkVehicleStatus(@PathVariable String vehicleNumber) {
        try {
            Optional<Booking> activeBooking = exitService.findActiveBookingByVehicleNumber(vehicleNumber.toUpperCase());
            StringBuilder result = new StringBuilder();
            result.append("Vehicle: ").append(vehicleNumber.toUpperCase()).append("\n");
            
            if (activeBooking.isPresent()) {
                Booking booking = activeBooking.get();
                result.append("Status: Currently ACTIVE booking found\n");
                result.append("Booking ID: ").append(booking.getId()).append("\n");
                result.append("Booking Code: ").append(booking.getBookingCode()).append("\n");
                result.append("Active: ").append(booking.getIsActive()).append("\n");
                result.append("Exit Time: ").append(booking.getExitTime()).append("\n");
            } else {
                result.append("Status: No active booking found (booking allowed)\n");
            }
            
            return ResponseEntity.ok(result.toString());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }
    
    /**
     * Debug endpoint to list all bookings
     */
    @GetMapping("/debug/bookings")
    public ResponseEntity<String> debugBookings() {
        try {
            List<Booking> allBookings = exitService.getAllBookings();
            StringBuilder result = new StringBuilder();
            result.append("Total bookings: ").append(allBookings.size()).append("\n\n");
            
            for (Booking booking : allBookings) {
                result.append("ID: ").append(booking.getId())
                       .append(", Code: ").append(booking.getBookingCode())
                       .append(", Vehicle: ").append(booking.getVehicleNumber())
                       .append(", Active: ").append(booking.getIsActive())
                       .append(", Exit Time: ").append(booking.getExitTime())
                       .append("\n");
            }
            
            return ResponseEntity.ok(result.toString());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
    
    /**
     * Convert exit details to sync format with PII protection
     */
    private Map<String, Object> convertExitDetailsToSyncFormat(Map<String, Object> exitDetails) {
        Map<String, Object> syncData = new java.util.HashMap<>();
        
        // Extract relevant fields
        syncData.put("id", exitDetails.get("id"));
        syncData.put("bookingCode", exitDetails.get("bookingCode"));
        syncData.put("vehicleNumber", exitDetails.get("vehicleNumber"));
        syncData.put("customerName", exitDetails.get("customerName"));
        
        // PII Protection: Remove or mask phone number
        Object phoneNumber = exitDetails.get("phoneNumber");
        if (phoneNumber != null && phoneNumber instanceof String) {
            String phone = (String) phoneNumber;
            if (phone.length() >= 4) {
                // Keep only last 4 digits
                syncData.put("phoneNumber", "***-***-" + phone.substring(phone.length() - 4));
            } else {
                syncData.put("phoneNumber", "***-***-****");
            }
        } else {
            syncData.put("phoneNumber", "***-***-****");
        }
        
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
