package com.smartparking.service;

import com.smartparking.entity.Booking;
import com.smartparking.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class ReceiptService {

    @Autowired
    private BookingRepository bookingRepository;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ReceiptService.class);

    public byte[] generateReceipt(String bookingIdentifier) throws IOException {
        log.info("Receipt requested for booking identifier: {}", bookingIdentifier);

        Booking booking;

        // Try to find by booking code first (new bookings) using fetch join to avoid
        // LazyInitializationException
        Optional<Booking> bookingByCode = bookingRepository.findByBookingCodeWithParkingSlot(bookingIdentifier);
        if (bookingByCode.isPresent()) {
            booking = bookingByCode.get();
        } else {
            // If not found by code, try to find by ID (legacy bookings) using fetch join
            try {
                Long bookingId = Long.parseLong(bookingIdentifier);
                booking = bookingRepository.findByIdWithParkingSlot(bookingId)
                        .orElseThrow(() -> new RuntimeException("Booking not found with ID: " + bookingId));
            } catch (NumberFormatException e) {
                log.error("Invalid booking identifier format: {}", bookingIdentifier);
                throw new RuntimeException("Invalid booking identifier: " + bookingIdentifier);
            }
        }

        log.info("Generating receipt for booking: ID={}, Code={}, Vehicle={}, Slot={}, Payment={}, ExitTime={}",
                booking.getId(), booking.getBookingCode(), booking.getVehicleNumber(),
                booking.getParkingSlot() != null ? booking.getParkingSlot().getSlotId() : "N/A",
                booking.getPaymentMethod(), booking.getExitTime());

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            // Generate enhanced receipt content
            String receiptContent = generateReceiptText(booking);
            outputStream.write(receiptContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            log.info("Receipt generated successfully for booking {}", booking.getId());
            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error("Receipt generation failed for booking {}: {}", booking.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to generate receipt: " + e.getMessage());
        }
    }

    private String generateReceiptText(Booking booking) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        StringBuilder receipt = new StringBuilder();
        receipt.append("========================================\n");
        receipt.append("        SMART PARKING RECEIPT        \n");
        receipt.append("========================================\n\n");

        receipt.append("BOOKING CODE: ").append(booking.getBookingCode() != null ? booking.getBookingCode() : "N/A")
                .append("\n");
        receipt.append("VEHICLE NUMBER: ").append(booking.getVehicleNumber()).append("\n");
        receipt.append("CUSTOMER NAME: ").append(booking.getCustomerName()).append("\n");
        receipt.append("PHONE NUMBER: ").append(booking.getPhoneNumber()).append("\n");
        receipt.append("VEHICLE TYPE: ").append(booking.getVehicleType()).append("\n");

        if (booking.getParkingSlot() != null) {
            receipt.append("SLOT ID: ").append(booking.getParkingSlot().getSlotId()).append("\n");
            receipt.append("FLOOR: ").append(booking.getParkingSlot().getFloor()).append("\n");
        }

        receipt.append("BOOKING TIME: ").append(booking.getBookingTime().format(formatter)).append("\n");

        if (booking.getExitTime() != null) {
            receipt.append("EXIT TIME: ").append(booking.getExitTime().format(formatter)).append("\n");

            long totalMinutes = java.time.Duration.between(booking.getBookingTime(), booking.getExitTime()).toMinutes();
            receipt.append("DURATION: ").append(formatDuration(totalMinutes)).append("\n");
        }

        if (booking.getPaymentMethod() != null) {
            receipt.append("PAYMENT METHOD: ").append(booking.getPaymentMethod()).append("\n");
            if (booking.getPaymentTime() != null) {
                receipt.append("PAYMENT TIME: ").append(booking.getPaymentTime().format(formatter)).append("\n");
            }
            if (booking.getTransactionId() != null && !booking.getTransactionId().isEmpty()) {
                receipt.append("TRANSACTION ID: ").append(booking.getTransactionId()).append("\n");
            }
        }

        if (booking.getParkingFee() != null) {
            receipt.append("PARKING FEE: ₹").append(booking.getParkingFee()).append("\n");
        }

        receipt.append("\n========================================\n");
        receipt.append("          THANK YOU FOR USING           \n");
        receipt.append("        SMART PARKING SYSTEM            \n");
        receipt.append("========================================\n");

        return receipt.toString();
    }

    /**
     * Format duration in minutes to human-readable format
     */
    private String formatDuration(long totalMinutes) {
        if (totalMinutes < 60) {
            return totalMinutes + " minute" + (totalMinutes == 1 ? "" : "s");
        } else {
            long hours = totalMinutes / 60;
            long minutes = totalMinutes % 60;
            if (minutes == 0) {
                return hours + " hour" + (hours == 1 ? "" : "s");
            } else {
                return hours + " hour" + (hours == 1 ? "" : "s") + " " + minutes + " minute"
                        + (minutes == 1 ? "" : "s");
            }
        }
    }
}
