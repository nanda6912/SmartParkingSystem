package com.smartparking.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for generating UPI payment QR codes
 */
@Service
public class UPIPaymentService {
    
    private static final Logger log = LoggerFactory.getLogger(UPIPaymentService.class);
    
    @Value("${parking.upi.merchant-id:smartparking@ybl}")
    private String merchantUpiId;
    
    @Value("${parking.upi.merchant-name:Smart Parking}")
    private String merchantName;
    
    /**
     * Generate UPI payment URI
     */
    public String generateUPIPaymentURI(String bookingCode, double amount, String note) {
        try {
            // Validate UPI configuration
            validateUPIConfiguration();
            
            // Validate input parameters
            if (bookingCode == null || bookingCode.trim().isEmpty()) {
                throw new IllegalArgumentException("Booking code cannot be null or empty");
            }
            if (amount <= 0) {
                throw new IllegalArgumentException("Amount must be greater than 0");
            }
            
            // Format: upi://pay?pa=<UPI_ID>&pn=<NAME>&am=<AMOUNT>&cu=INR&tn=<NOTE>
            // Use proper URL encoding for UPI compatibility
            String encodedMerchantName = java.net.URLEncoder.encode(merchantName, "UTF-8");
            String encodedNote = java.net.URLEncoder.encode(note, "UTF-8");
            
            String upiUri = String.format(
                "upi://pay?pa=%s&pn=%s&am=%.2f&cu=INR&tn=%s",
                merchantUpiId,
                encodedMerchantName,
                amount,
                encodedNote
            );
            
            log.info("Generated UPI URI for booking {}: {}", bookingCode, upiUri);
            log.info("UPI Payment Details - Merchant: {}, UPI ID: {}, Amount: ₹{}", 
                     merchantName, merchantUpiId, amount);
            
            return upiUri;
            
        } catch (IllegalArgumentException e) {
            log.error("Invalid input for UPI URI generation: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error generating UPI URI for booking {}: {}", bookingCode, e.getMessage(), e);
            throw new RuntimeException("Failed to generate UPI payment URI", e);
        }
    }
    
    /**
     * Validate UPI configuration
     */
    private void validateUPIConfiguration() {
        if (merchantUpiId == null || merchantUpiId.trim().isEmpty()) {
            log.error("UPI Merchant ID is not configured. Please set parking.upi.merchant-id in application properties");
            throw new IllegalStateException("UPI payment is not configured properly. Contact administrator.");
        }
        
        if (merchantName == null || merchantName.trim().isEmpty()) {
            log.error("UPI Merchant Name is not configured. Please set parking.upi.merchant-name in application properties");
            throw new IllegalStateException("UPI payment is not configured properly. Contact administrator.");
        }
        
        // Basic UPI ID validation (should contain @ and valid format)
        if (!merchantUpiId.contains("@")) {
            log.error("Invalid UPI ID format: {}. UPI ID must contain @ symbol", merchantUpiId);
            throw new IllegalStateException("UPI payment is not configured with valid UPI ID. Contact administrator.");
        }
        
        log.debug("UPI Configuration validated - Merchant: {}, UPI ID: {}", merchantName, merchantUpiId);
    }
    
    /**
     * Generate QR code image from UPI URI
     */
    public byte[] generateQRCodeImage(String upiUri) {
        try {
            // Validate UPI URI
            if (upiUri == null || upiUri.trim().isEmpty()) {
                throw new IllegalArgumentException("UPI URI cannot be null or empty");
            }
            
            // Check URI length (UPI URIs have reasonable length limits)
            if (upiUri.length() > 500) {
                throw new IllegalArgumentException("UPI URI is too long");
            }
            
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(upiUri, BarcodeFormat.QR_CODE, 300, 300);
            
            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            
            byte[] pngData = pngOutputStream.toByteArray();
            log.info("Generated QR code image successfully, size: {} bytes", pngData.length);
            log.debug("QR code generated for UPI URI: {}", upiUri);
            
            return pngData;
            
        } catch (IllegalArgumentException e) {
            log.error("Invalid input for QR code generation: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error generating QR code image: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate QR code image", e);
        }
    }
    
    /**
     * Generate QR code as Base64 string for frontend display
     */
    public String generateQRCodeBase64(String upiUri) {
        byte[] qrCodeImage = generateQRCodeImage(upiUri);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(qrCodeImage);
    }
    
    /**
     * Generate complete payment response with QR code
     */
    public PaymentQRResponse generatePaymentQR(String bookingCode, double amount, String customerName) {
        try {
            // Validate input parameters
            if (bookingCode == null || bookingCode.trim().isEmpty()) {
                throw new IllegalArgumentException("Booking code cannot be null or empty");
            }
            if (amount <= 0) {
                throw new IllegalArgumentException("Amount must be greater than 0");
            }
            
            log.info("Generating payment QR for booking: {}, amount: ₹{}, customer: {}", 
                     bookingCode, amount, customerName);
            
            String note = String.format("Parking Fee - Booking %s - %s", 
                bookingCode, 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")));
            
            String upiUri = generateUPIPaymentURI(bookingCode, amount, note);
            String qrCodeBase64 = generateQRCodeBase64(upiUri);
            
            PaymentQRResponse response = new PaymentQRResponse(
                bookingCode,
                amount,
                merchantUpiId,
                merchantName,
                upiUri,
                qrCodeBase64,
                note
            );
            
            log.info("Successfully generated payment QR for booking {}: URI length: {}, QR size: {} bytes", 
                     bookingCode, upiUri.length(), qrCodeBase64.length());
            
            return response;
            
        } catch (IllegalArgumentException e) {
            log.error("Invalid input for payment QR generation: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error generating payment QR for booking {}: {}", bookingCode, e.getMessage(), e);
            throw new RuntimeException("Failed to generate payment QR", e);
        }
    }
    
    /**
     * Validate UPI transaction ID format (last 5 digits)
     */
    public boolean validateTransactionId(String transactionId) {
        if (transactionId == null || transactionId.trim().isEmpty()) {
            return false;
        }
        
        // Accept only last 5 digits of transaction ID
        String trimmedId = transactionId.trim();
        return trimmedId.matches("^[0-9]{5}$");
    }
    
    /**
     * Check if UPI configuration is properly set up
     */
    public boolean isUPIConfigurationValid() {
        try {
            validateUPIConfiguration();
            return true;
        } catch (Exception e) {
            log.warn("UPI configuration validation failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Get UPI configuration status for health check
     */
    public Map<String, Object> getUPIConfigurationStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("configured", isUPIConfigurationValid());
        if (merchantUpiId != null) {
            status.put("merchantId", merchantUpiId.substring(0, Math.min(3, merchantUpiId.length())) + "***");
        } else {
            status.put("merchantId", "Not configured");
        }
        status.put("merchantName", merchantName);
        status.put("timestamp", LocalDateTime.now().toString());
        return status;
    }
    
    /**
     * Response DTO for payment QR generation
     */
    public static class PaymentQRResponse {
        private String bookingCode;
        private double amount;
        private String merchantUpiId;
        private String merchantName;
        private String upiUri;
        private String qrCodeBase64;
        private String note;
        private LocalDateTime generatedAt;
        
        public PaymentQRResponse(String bookingCode, double amount, String merchantUpiId, 
                               String merchantName, String upiUri, String qrCodeBase64, String note) {
            this.bookingCode = bookingCode;
            this.amount = amount;
            this.merchantUpiId = merchantUpiId;
            this.merchantName = merchantName;
            this.upiUri = upiUri;
            this.qrCodeBase64 = qrCodeBase64;
            this.note = note;
            this.generatedAt = LocalDateTime.now();
        }
        
        // Getters
        public String getBookingCode() { return bookingCode; }
        public double getAmount() { return amount; }
        public String getMerchantUpiId() { return merchantUpiId; }
        public String getMerchantName() { return merchantName; }
        public String getUpiUri() { return upiUri; }
        public String getQrCodeBase64() { return qrCodeBase64; }
        public String getNote() { return note; }
        public LocalDateTime getGeneratedAt() { return generatedAt; }
        
        // Setters
        public void setBookingCode(String bookingCode) { this.bookingCode = bookingCode; }
        public void setAmount(double amount) { this.amount = amount; }
        public void setMerchantUpiId(String merchantUpiId) { this.merchantUpiId = merchantUpiId; }
        public void setMerchantName(String merchantName) { this.merchantName = merchantName; }
        public void setUpiUri(String upiUri) { this.upiUri = upiUri; }
        public void setQrCodeBase64(String qrCodeBase64) { this.qrCodeBase64 = qrCodeBase64; }
        public void setNote(String note) { this.note = note; }
        public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
    }
}
