package com.smartparking.service;

import com.smartparking.entity.Booking;
import com.smartparking.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
public class ReceiptService {
    
    @Autowired
    private BookingRepository bookingRepository;
    
    public byte[] generateReceipt(String bookingIdentifier) throws IOException {
        Booking booking;
        
        // Try to find by booking code first (new bookings)
        Optional<Booking> bookingByCode = bookingRepository.findByBookingCode(bookingIdentifier);
        if (bookingByCode.isPresent()) {
            booking = bookingByCode.get();
        } else {
            // If not found by code, try to find by ID (legacy bookings)
            try {
                Long bookingId = Long.parseLong(bookingIdentifier);
                booking = bookingRepository.findById(bookingId)
                        .orElseThrow(() -> new RuntimeException("Booking not found"));
            } catch (NumberFormatException e) {
                throw new RuntimeException("Invalid booking identifier: " + bookingIdentifier);
            }
        }
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        // Simple text-based receipt (can be enhanced with PDF library later)
        String receiptContent = generateReceiptText(booking);
        outputStream.write(receiptContent.getBytes());
        
        return outputStream.toByteArray();
    }
    
    private String generateReceiptText(Booking booking) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        StringBuilder receipt = new StringBuilder();
        receipt.append("========================================\n");
        receipt.append("        SMART PARKING RECEIPT        \n");
        receipt.append("========================================\n\n");
        
        // Use booking code if available, otherwise fall back to ID for backward compatibility
        String bookingIdentifier = booking.getBookingCode() != null ? 
            booking.getBookingCode() : String.valueOf(booking.getId());
        
        receipt.append("BOOKING CODE: ").append(bookingIdentifier).append("\n");
        receipt.append("VEHICLE NUMBER: ").append(booking.getVehicleNumber()).append("\n");
        receipt.append("CUSTOMER NAME: ").append(booking.getCustomerName()).append("\n");
        receipt.append("PHONE NUMBER: ").append(booking.getPhoneNumber()).append("\n");
        receipt.append("VEHICLE TYPE: ").append(booking.getVehicleType()).append("\n");
        receipt.append("SLOT NUMBER: ").append(booking.getParkingSlot().getSlotNumber()).append("\n");
        receipt.append("FLOOR: ").append(booking.getParkingSlot().getFloor()).append("\n");
        receipt.append("BOOKING TIME: ").append(booking.getBookingTime().format(formatter)).append("\n");
        
        if (booking.getExitTime() != null) {
            receipt.append("EXIT TIME: ").append(booking.getExitTime().format(formatter)).append("\n");
            
            // Add duration in human-readable format
            long totalMinutes = java.time.Duration.between(booking.getBookingTime(), booking.getExitTime()).toMinutes();
            receipt.append("DURATION: ").append(formatDuration(totalMinutes)).append("\n");
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
                return hours + " hour" + (hours == 1 ? "" : "s") + " " + minutes + " minute" + (minutes == 1 ? "" : "s");
            }
        }
    }
}
