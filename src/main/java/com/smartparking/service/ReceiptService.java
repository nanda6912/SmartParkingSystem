package com.smartparking.service;

import com.smartparking.entity.Booking;
import com.smartparking.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

@Service
public class ReceiptService {
    
    @Autowired
    private BookingRepository bookingRepository;
    
    public byte[] generateReceipt(String bookingCode) throws IOException {
        Booking booking = bookingRepository.findByBookingCode(bookingCode)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        
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
        
        receipt.append("BOOKING CODE: ").append(booking.getBookingCode()).append("\n");
        receipt.append("VEHICLE NUMBER: ").append(booking.getVehicleNumber()).append("\n");
        receipt.append("CUSTOMER NAME: ").append(booking.getCustomerName()).append("\n");
        receipt.append("PHONE NUMBER: ").append(booking.getPhoneNumber()).append("\n");
        receipt.append("VEHICLE TYPE: ").append(booking.getVehicleType()).append("\n");
        receipt.append("SLOT NUMBER: ").append(booking.getParkingSlot().getSlotNumber()).append("\n");
        receipt.append("FLOOR: ").append(booking.getParkingSlot().getFloor()).append("\n");
        receipt.append("BOOKING TIME: ").append(booking.getBookingTime().format(formatter)).append("\n");
        
        if (booking.getExitTime() != null) {
            receipt.append("EXIT TIME: ").append(booking.getExitTime().format(formatter)).append("\n");
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
}
