package com.smartparking.controller;

import com.smartparking.entity.Booking;
import com.smartparking.service.ExitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Controller for vehicle exit and fee management
 */
@RestController
@RequestMapping("/api/exit")
public class ExitController {
    
    @Autowired
    private ExitService exitService;
    
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
            Map<String, Object> exitDetails = exitService.processExit(bookingId);
            return ResponseEntity.ok(exitDetails);
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
                    .orElseThrow(() -> new RuntimeException("Booking not found"));
            
            // Generate receipt content
            String receiptContent = exitService.generateExitReceipt(booking);
            
            // Create downloadable file
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
            String filename = "exit_receipt_" + bookingId + "_" + LocalDateTime.now().format(formatter) + ".txt";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            headers.setContentDispositionFormData("attachment", filename);
            
            return new ResponseEntity<>(receiptContent.getBytes(), headers, HttpStatus.OK);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get exit statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getExitStats() {
        try {
            Map<String, Object> stats = exitService.getExitStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
