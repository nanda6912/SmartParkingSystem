package com.smartparking.controller;

import com.smartparking.entity.Booking;
import com.smartparking.service.ExitService;
import com.smartparking.service.DataSyncService;
import com.smartparking.service.UPIPaymentService;
import com.smartparking.service.ReceiptService;
import com.smartparking.repository.BookingRepository;
import com.smartparking.exception.BookingNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.HashMap;
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

    private static final Logger log = LoggerFactory.getLogger(ExitController.class);

    @Autowired
    private ExitService exitService;

    @Autowired
    private DataSyncService dataSyncService;

    @Autowired
    private UPIPaymentService upiPaymentService;

    @Autowired
    private ReceiptService receiptService;

    @Autowired
    private BookingRepository bookingRepository;

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
                    "message", e.getMessage()));
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
            response.put("success", true);
            response.put("bookingId", exitDetails.get("bookingId"));
            response.put("bookingCode", exitDetails.get("bookingCode"));
            response.put("vehicleNumber", exitDetails.get("vehicleNumber"));
            response.put("customerName", exitDetails.get("customerName"));
            response.put("phoneNumber", exitDetails.get("phoneNumber"));
            response.put("vehicleType", exitDetails.get("vehicleType"));
            response.put("slotNumber", exitDetails.get("slotNumber"));
            response.put("floor", exitDetails.get("floor"));
            response.put("entryTime", exitDetails.get("entryTime"));
            response.put("exitTime", exitDetails.get("exitTime"));
            response.put("duration", exitDetails.get("duration"));
            response.put("hoursCharged", exitDetails.get("hoursCharged"));
            response.put("totalFee", exitDetails.get("totalFee"));
            response.put("syncStatus", syncStatus);
            response.put("syncError", syncError);
            response.put("timestamp", LocalDateTime.now());
            response.put("message", "Exit processed successfully");

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            // Handle specific business logic errors
            log.error("Business logic error processing exit for booking {}: {}", bookingId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to process exit",
                    "message", e.getMessage(),
                    "bookingId", bookingId));
        } catch (Exception e) {
            // Handle unexpected errors
            log.error("Unexpected error processing exit for booking {}: {}", bookingId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Internal server error",
                    "message", "An unexpected error occurred while processing the exit",
                    "bookingId", bookingId));
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
                    "message", e.getMessage()));
        }
    }

    /**
     * Download exit receipt
     */
    /**
     * Download exit receipt
     */
    @GetMapping("/receipt/{bookingId}")
    public ResponseEntity<?> downloadExitReceipt(@PathVariable Long bookingId) {
        log.info("Receipt requested for booking ID: {}", bookingId);

        try {
            // Find booking by ID first to check existence
            // Use findByIdWithParkingSlot to ensure slot is available for logging
            Booking booking = bookingRepository.findByIdWithParkingSlot(bookingId)
                    .orElseThrow(() -> new BookingNotFoundException("Booking not found with ID: " + bookingId));

            log.info("Receipt generation started for Booking ID: {}, Vehicle: {}, Slot: {}, Payment: {}",
                    booking.getId(), booking.getVehicleNumber(),
                    booking.getParkingSlot() != null ? booking.getParkingSlot().getSlotId() : "N/A",
                    booking.getPaymentMethod());

            // Use ReceiptService to generate bytes
            byte[] receiptBytes = receiptService.generateReceipt(String.valueOf(bookingId));

            // Create downloadable file
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
            String filename = "exit_receipt_" + bookingId + "_" + LocalDateTime.now().format(formatter) + ".txt";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            headers.setContentDisposition(ContentDisposition.builder("attachment").filename(filename).build());
            headers.setContentLength(receiptBytes.length);

            log.info("Receipt generated successfully for booking {}", bookingId);
            return new ResponseEntity<>(receiptBytes, headers, HttpStatus.OK);

        } catch (BookingNotFoundException e) {
            log.warn("Booking not found during receipt request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "error", "Booking not found",
                    "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Receipt generation failed for booking ID {}: {}", bookingId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "error", "Receipt generation error",
                    "message", "An error occurred while generating the receipt: " + e.getMessage()));
        }
    }

    @GetMapping("/receipt/by-code/{bookingCode}")
    public ResponseEntity<?> downloadExitReceiptByCode(@PathVariable String bookingCode) {
        log.info("Receipt requested for booking code: {}", bookingCode);

        try {
            // Check existence first
            Booking booking = bookingRepository.findByBookingCodeWithParkingSlot(bookingCode)
                    .orElseThrow(() -> new BookingNotFoundException("Booking not found with code: " + bookingCode));

            log.info("Receipt generation started for Booking Code: {}, Vehicle: {}, Slot: {}",
                    booking.getBookingCode(), booking.getVehicleNumber(),
                    booking.getParkingSlot() != null ? booking.getParkingSlot().getSlotId() : "N/A");

            byte[] receiptBytes = receiptService.generateReceipt(bookingCode);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
            String filename = "exit_receipt_" + bookingCode + "_" + LocalDateTime.now().format(formatter) + ".txt";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            headers.setContentDisposition(ContentDisposition.builder("attachment").filename(filename).build());
            headers.setContentLength(receiptBytes.length);

            log.info("Receipt generated successfully for booking code {}", bookingCode);
            return new ResponseEntity<>(receiptBytes, headers, HttpStatus.OK);

        } catch (BookingNotFoundException e) {
            log.warn("Booking not found during receipt request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "error", "Booking not found",
                    "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Receipt generation failed for booking code {}: {}", bookingCode, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "error", "Receipt generation error",
                    "message", "An error occurred while generating the receipt: " + e.getMessage()));
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
     * Get synchronized exit data for frontend
     */
    @GetMapping("/sync-data")
    public ResponseEntity<Map<String, Object>> getExitSyncData() {
        try {
            Map<String, Object> syncData = dataSyncService.getExitPageSyncData();
            return ResponseEntity.ok(syncData);
        } catch (Exception e) {
            System.err.println("Error fetching exit sync data: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "Failed to fetch exit sync data"));
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
        syncData.put("id", exitDetails.get("bookingId")); // Fixed: use bookingId instead of id
        syncData.put("bookingCode", exitDetails.get("bookingCode"));
        syncData.put("vehicleNumber", exitDetails.get("vehicleNumber"));

        // PII Protection: Mask customer name
        Object customerName = exitDetails.get("customerName");
        if (customerName != null && customerName instanceof String) {
            String name = (String) customerName;
            if (name.length() > 2) {
                // Keep first and last character, mask middle
                syncData.put("customerName", name.substring(0, 1) + "***" + name.substring(name.length() - 1));
            } else {
                syncData.put("customerName", "***");
            }
        } else {
            syncData.put("customerName", "***");
        }

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
        syncData.put("parkingFee", exitDetails.get("totalFee")); // Fixed: use totalFee instead of parkingFee
        syncData.put("duration", exitDetails.get("duration"));
        syncData.put("processedAt", LocalDateTime.now());

        return syncData;
    }

    /**
     * Generate UPI payment QR code for a booking
     */
    @GetMapping("/generate-upi-qr/{bookingId}")
    public ResponseEntity<Map<String, Object>> generateUPIQR(@PathVariable Long bookingId) {
        try {
            // First calculate fee for the booking
            Map<String, Object> feeDetails = exitService.calculateFee(bookingId);

            if (feeDetails.containsKey("error")) {
                return ResponseEntity.badRequest().body(feeDetails);
            }

            // Get booking details for customer name
            Optional<Booking> bookingOpt = exitService.findBookingById(bookingId);
            if (bookingOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Booking not found",
                        "message", "No booking found with ID: " + bookingId));
            }
            Booking booking = bookingOpt.get();

            // Generate UPI payment QR
            double amount = (Double) feeDetails.get("totalFee");
            String bookingCode = (String) feeDetails.get("bookingCode");
            String customerName = booking.getCustomerName();

            UPIPaymentService.PaymentQRResponse qrResponse = upiPaymentService.generatePaymentQR(
                    bookingCode, amount, customerName);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("bookingId", bookingId);
            response.put("bookingCode", qrResponse.getBookingCode());
            response.put("amount", qrResponse.getAmount());
            response.put("merchantUpiId", qrResponse.getMerchantUpiId());
            response.put("merchantName", qrResponse.getMerchantName());
            response.put("upiUri", qrResponse.getUpiUri());
            response.put("qrCodeBase64", qrResponse.getQrCodeBase64());
            response.put("note", qrResponse.getNote());
            response.put("generatedAt", qrResponse.getGeneratedAt());
            response.put("message", "UPI QR code generated successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error generating UPI QR for bookingId {}: {}", bookingId, e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to generate UPI QR code",
                    "message", e.getMessage()));
        }
    }

    /**
     * Process payment and complete exit
     */
    @PostMapping("/process-payment/{bookingId}")
    public ResponseEntity<Map<String, Object>> processPayment(@PathVariable Long bookingId,
            @RequestBody Map<String, Object> paymentData) {
        try {
            String paymentMethod = (String) paymentData.get("paymentMethod");
            String transactionId = (String) paymentData.get("transactionId");

            // Validate payment method
            if (!"UPI".equalsIgnoreCase(paymentMethod) && !"CASH".equalsIgnoreCase(paymentMethod)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Invalid payment method",
                        "message", "Payment method must be either UPI or CASH"));
            }

            // For UPI payments, transaction ID is required
            if ("UPI".equalsIgnoreCase(paymentMethod)) {
                if (!upiPaymentService.validateTransactionId(transactionId)) {
                    return ResponseEntity.badRequest().body(Map.of(
                            "error", "Invalid transaction ID",
                            "message", "UPI payments require a valid transaction ID (last 5 digits only)"));
                }
            }

            // Process exit with payment details
            Map<String, Object> exitDetails = exitService.processExitWithPayment(bookingId, paymentMethod,
                    transactionId);

            // Try to sync data but don't fail the entire operation if sync fails
            String syncStatus = "success";
            String syncError = null;

            try {
                Map<String, Object> syncExitData = convertExitDetailsToSyncFormat(exitDetails);
                dataSyncService.addExitRecord(syncExitData);
            } catch (Exception syncEx) {
                System.err.println("Sync operation failed for bookingId " + bookingId + ": " + syncEx.getMessage());
                syncEx.printStackTrace();
                syncStatus = "failed";
                syncError = "Data synchronization failed";
            }

            // Return combined response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("bookingId", exitDetails.get("bookingId"));
            response.put("bookingCode", exitDetails.get("bookingCode"));
            response.put("vehicleNumber", exitDetails.get("vehicleNumber"));
            response.put("customerName", exitDetails.get("customerName"));
            response.put("phoneNumber", exitDetails.get("phoneNumber"));
            response.put("vehicleType", exitDetails.get("vehicleType"));
            response.put("slotNumber", exitDetails.get("slotNumber"));
            response.put("floor", exitDetails.get("floor"));
            response.put("entryTime", exitDetails.get("entryTime"));
            response.put("exitTime", exitDetails.get("exitTime"));
            response.put("duration", exitDetails.get("duration"));
            response.put("hoursCharged", exitDetails.get("hoursCharged"));
            response.put("totalFee", exitDetails.get("totalFee"));
            response.put("paymentMethod", exitDetails.get("paymentMethod"));
            response.put("transactionId", exitDetails.get("transactionId"));
            response.put("paymentTime", exitDetails.get("paymentTime"));
            response.put("syncStatus", syncStatus);
            response.put("syncError", syncError);
            response.put("timestamp", LocalDateTime.now());
            response.put("message", "Payment processed and exit completed successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error processing payment for bookingId {}: {}", bookingId, e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to process payment",
                    "message", e.getMessage()));
        }
    }
}
