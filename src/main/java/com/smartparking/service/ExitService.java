package com.smartparking.service;

import com.smartparking.entity.Booking;
import com.smartparking.entity.ParkingSlot;
import com.smartparking.repository.BookingRepository;
import com.smartparking.repository.ParkingSlotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing vehicle exits and fee calculations
 */
@Service
@Transactional
public class ExitService {
    
    private static final Logger log = LoggerFactory.getLogger(ExitService.class);
    private static final double HOURLY_RATE = 20.0;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    @Autowired
    private BookingRepository bookingRepository;
    
    @Autowired
    private ParkingSlotRepository parkingSlotRepository;
    
    /**
     * Get all active bookings for exit management
     */
    public List<Map<String, Object>> getActiveBookingsForExit() {
        try {
            List<Booking> activeBookings = bookingRepository.findActiveBookingsWithoutExit();
            
            return activeBookings.stream()
                    .sorted(Comparator.comparing(Booking::getBookingTime))
                    .map(this::convertToExitDTO)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("Error getting active bookings for exit: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Calculate parking fee for a booking
     */
    public Map<String, Object> calculateFee(Long bookingId) {
        try {
            Booking booking = findBookingById(bookingId)
                    .orElseThrow(() -> new RuntimeException("Booking not found"));
            
            if (booking.getExitTime() != null) {
                throw new RuntimeException("Vehicle has already exited");
            }
            
            LocalDateTime now = LocalDateTime.now();
            Duration duration = Duration.between(booking.getBookingTime(), now);
            
            // Calculate hours (round up to next hour)
            long totalMinutes = duration.toMinutes();
            long hours = (totalMinutes + 59) / 60; // Round up to next hour
            double fee = hours * HOURLY_RATE;
            
            Map<String, Object> feeDetails = new HashMap<>();
            feeDetails.put("bookingId", booking.getId());
            
            // Handle null/empty booking codes for backward compatibility
            String displayBookingCode = (booking.getBookingCode() != null && !booking.getBookingCode().isEmpty()) ? 
                booking.getBookingCode() : "ID-" + booking.getId();
            feeDetails.put("bookingCode", displayBookingCode);
            
            feeDetails.put("vehicleNumber", booking.getVehicleNumber());
            feeDetails.put("customerName", booking.getCustomerName());
            feeDetails.put("entryTime", booking.getBookingTime().format(FORMATTER));
            feeDetails.put("exitTime", now.format(FORMATTER));
            feeDetails.put("totalMinutes", totalMinutes);
            feeDetails.put("hoursCharged", hours);
            feeDetails.put("hourlyRate", HOURLY_RATE);
            feeDetails.put("totalFee", fee);
            feeDetails.put("slotNumber", booking.getParkingSlot().getSlotNumber());
            feeDetails.put("floor", booking.getParkingSlot().getFloor());
            
            return feeDetails;
            
        } catch (Exception e) {
            log.error("Error calculating fee for booking {}: {}", bookingId, e.getMessage(), e);
            throw new RuntimeException("Failed to calculate fee: " + e.getMessage());
        }
    }
    
    /**
     * Process vehicle exit and update booking
     */
    public Map<String, Object> processExit(Long bookingId) {
        try {
            Booking booking = findBookingById(bookingId)
                    .orElseThrow(() -> new RuntimeException("Booking not found"));
            
            if (booking.getExitTime() != null) {
                throw new RuntimeException("Vehicle has already exited");
            }
            
            // Check if booking is still active
            if (booking.getIsActive() == false) {
                throw new RuntimeException("Booking is already processed");
            }
            
            LocalDateTime now = LocalDateTime.now();
            Duration duration = Duration.between(booking.getBookingTime(), now);
            
            // Calculate hours (round up to next hour)
            long totalMinutes = duration.toMinutes();
            long hours = (totalMinutes + 59) / 60; // Round up to next hour
            double fee = hours * HOURLY_RATE;
            
            // Update booking
            booking.setExitTime(now);
            booking.setParkingFee((int) Math.round(fee));
            booking.setIsActive(false);
            
            try {
                Booking savedBooking = bookingRepository.save(booking);
                
                // Update slot status only after booking is successfully saved
                ParkingSlot slot = booking.getParkingSlot();
                slot.setStatus(com.smartparking.enums.SlotStatus.AVAILABLE);
                slot.setLockUntil(null);
                parkingSlotRepository.save(slot);
                
                // Prepare response
                Map<String, Object> exitDetails = new HashMap<>();
                exitDetails.put("success", true);
                exitDetails.put("message", "Vehicle exit processed successfully");
                exitDetails.put("bookingId", savedBooking.getId());
                
                // Handle null/empty booking codes for backward compatibility
                String displayBookingCode = (savedBooking.getBookingCode() != null && !savedBooking.getBookingCode().isEmpty()) ? 
                    savedBooking.getBookingCode() : "ID-" + savedBooking.getId();
                exitDetails.put("bookingCode", displayBookingCode);
                exitDetails.put("vehicleNumber", savedBooking.getVehicleNumber());
                exitDetails.put("customerName", savedBooking.getCustomerName());
                exitDetails.put("phoneNumber", savedBooking.getPhoneNumber());
                exitDetails.put("vehicleType", savedBooking.getVehicleType() != null ? savedBooking.getVehicleType().toString() : "UNKNOWN");
                exitDetails.put("entryTime", savedBooking.getBookingTime().format(FORMATTER));
                exitDetails.put("exitTime", savedBooking.getExitTime().format(FORMATTER));
                exitDetails.put("totalMinutes", totalMinutes);
                exitDetails.put("duration", formatDuration(totalMinutes)); // Add human-readable duration
                exitDetails.put("hoursCharged", hours);
                exitDetails.put("hourlyRate", HOURLY_RATE);
                exitDetails.put("totalFee", fee);
                exitDetails.put("slotNumber", slot.getSlotNumber());
                exitDetails.put("floor", slot.getFloor());
                exitDetails.put("slotReleased", true);
                
                log.info("Vehicle exit processed: Booking ID {}, Vehicle {}, Fee {}", 
                        bookingId, savedBooking.getVehicleNumber(), fee);
                
                return exitDetails;
                
            } catch (Exception e) {
                log.error("Error saving booking during exit processing: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to process exit: " + e.getMessage());
            }
            
        } catch (Exception e) {
            log.error("Error processing exit for booking {}: {}", bookingId, e.getMessage(), e);
            throw new RuntimeException("Failed to process exit: " + e.getMessage());
        }
    }
    
    /**
     * Generate exit receipt content
     */
    public String generateExitReceipt(Booking booking) {
        StringBuilder receipt = new StringBuilder();
        receipt.append("========================================\n");
        receipt.append("        SMART PARKING EXIT RECEIPT     \n");
        receipt.append("========================================\n\n");
        
        // Use booking code if available, otherwise use ID with prefix
        String bookingIdentifier;
        if (booking.getBookingCode() != null && !booking.getBookingCode().isEmpty()) {
            bookingIdentifier = booking.getBookingCode();
        } else {
            bookingIdentifier = "ID-" + booking.getId();
        }
        
        receipt.append("BOOKING CODE: ").append(bookingIdentifier).append("\n");
        receipt.append("VEHICLE NUMBER: ").append(booking.getVehicleNumber()).append("\n");
        receipt.append("CUSTOMER NAME: ").append(booking.getCustomerName()).append("\n");
        receipt.append("PHONE NUMBER: ").append(booking.getPhoneNumber()).append("\n");
        receipt.append("VEHICLE TYPE: ").append(booking.getVehicleType()).append("\n");
        receipt.append("SLOT NUMBER: ").append(booking.getParkingSlot().getSlotNumber()).append("\n");
        receipt.append("FLOOR: ").append(booking.getParkingSlot().getFloor()).append("\n");
        receipt.append("ENTRY TIME: ").append(booking.getBookingTime().format(FORMATTER)).append("\n");
        receipt.append("EXIT TIME: ").append(booking.getExitTime() != null ? booking.getExitTime().format(FORMATTER) : "Still Parked").append("\n");
        
        if (booking.getExitTime() != null) {
            Duration duration = Duration.between(booking.getBookingTime(), booking.getExitTime());
            long totalMinutes = duration.toMinutes();
            long hours = (totalMinutes + 59) / 60; // Round up
            double fee = booking.getParkingFee() != null ? booking.getParkingFee() : 0.0;
            
            receipt.append("PARKING DURATION: ").append(totalMinutes).append(" minutes (").append(hours).append(" hours)\n");
            receipt.append("HOURLY RATE: ₹").append(HOURLY_RATE).append("\n");
            receipt.append("TOTAL AMOUNT: ₹").append(String.format("%.2f", fee)).append("\n");
        } else {
            // For active bookings that haven't exited yet
            Duration duration = Duration.between(booking.getBookingTime(), LocalDateTime.now());
            long totalMinutes = duration.toMinutes();
            long hours = (totalMinutes + 59) / 60; // Round up
            double currentFee = hours * HOURLY_RATE;
            
            receipt.append("PARKING DURATION: ").append(totalMinutes).append(" minutes (").append(hours).append(" hours) [Ongoing]\n");
            receipt.append("HOURLY RATE: ₹").append(HOURLY_RATE).append("\n");
            receipt.append("CURRENT FEE: ₹").append(String.format("%.2f", currentFee)).append(" [Still Running]\n");
        }
        
        receipt.append("\n========================================\n");
        receipt.append("          THANK YOU FOR USING           \n");
        receipt.append("        SMART PARKING SYSTEM            \n");
        receipt.append("========================================\n");
        
        return receipt.toString();
    }
    
    /**
     * Get exit statistics
     */
    public Map<String, Object> getExitStatistics() {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // Today's exits
            LocalDateTime today = LocalDateTime.now().toLocalDate().atStartOfDay();
            List<Booking> todayExits = bookingRepository.findExitedBookingsFromDate(today);
            
            double todayRevenue = todayExits.stream()
                    .mapToDouble(b -> b.getParkingFee() != null ? b.getParkingFee() : 0.0)
                    .sum();
            
            // Total active bookings
            long activeBookings = bookingRepository.findActiveBookingsWithoutExit().size();
            
            stats.put("todayExits", todayExits.size());
            stats.put("todayRevenue", todayRevenue);
            stats.put("activeBookings", activeBookings);
            stats.put("hourlyRate", HOURLY_RATE);
            stats.put("timestamp", LocalDateTime.now().format(FORMATTER));
            
            return stats;
            
        } catch (Exception e) {
            log.error("Error getting exit statistics: {}", e.getMessage(), e);
            return Map.of("error", "Failed to get statistics");
        }
    }
    
    /**
     * Find active booking by vehicle number
     */
    public Optional<Booking> findActiveBookingByVehicleNumber(String vehicleNumber) {
        return bookingRepository.findByVehicleNumberAndIsActiveTrue(vehicleNumber);
    }
    
    /**
     * Get all bookings for debugging
     */
    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }
    
    /**
     * Find booking by ID
     */
    public Optional<Booking> findBookingById(Long bookingId) {
        return bookingRepository.findById(bookingId);
    }
    
    /**
     * Clean up old bookings with invalid booking codes (1-2 digit codes)
     * This method removes bookings that don't have 5-character alphanumeric codes
     */
    @Transactional
    public Map<String, Object> cleanupOldBookings() {
        try {
            List<Booking> allBookings = bookingRepository.findAll();
            List<Booking> toRemove = new ArrayList<>();
            int removedCount = 0;
            List<String> releasedSlots = new ArrayList<>();
            
            for (Booking booking : allBookings) {
                String code = booking.getBookingCode();
                // Remove bookings with null, empty, or numeric-only codes less than 5 characters
                if (code == null || code.isEmpty() || 
                    (code.matches("\\d+") && code.length() < 5)) {
                    
                    toRemove.add(booking);
                    
                    // Release the parking slot
                    ParkingSlot slot = booking.getParkingSlot();
                    if (slot != null) {
                        slot.setStatus(com.smartparking.enums.SlotStatus.AVAILABLE);
                        slot.setLockUntil(null);
                        parkingSlotRepository.save(slot);
                        releasedSlots.add("F" + slot.getFloor() + "-S" + slot.getSlotNumber());
                    }
                    
                    removedCount++;
                }
            }
            
            // Delete the problematic bookings
            for (Booking booking : toRemove) {
                bookingRepository.delete(booking);
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("removedCount", removedCount);
            result.put("releasedSlots", releasedSlots);
            result.put("message", "Cleaned up " + removedCount + " old bookings with invalid codes");
            
            log.info("Cleanup completed: Removed {} bookings, released slots: {}", 
                    removedCount, releasedSlots);
            
            return result;
            
        } catch (Exception e) {
            log.error("Error during cleanup: {}", e.getMessage(), e);
            return Map.of("success", false, "error", "Cleanup failed: " + e.getMessage());
        }
    }
    private Map<String, Object> convertToExitDTO(Booking booking) {
        Map<String, Object> dto = new HashMap<>();
        
        // Use booking code if available, otherwise ID
        String bookingIdentifier = booking.getBookingCode() != null ? 
            booking.getBookingCode() : String.valueOf(booking.getId());
        
        dto.put("bookingId", booking.getId());
        dto.put("bookingCode", bookingIdentifier);
        dto.put("vehicleNumber", booking.getVehicleNumber());
        dto.put("customerName", booking.getCustomerName());
        dto.put("phoneNumber", booking.getPhoneNumber());
        dto.put("vehicleType", booking.getVehicleType());
        dto.put("slotNumber", booking.getParkingSlot().getSlotNumber());
        dto.put("floor", booking.getParkingSlot().getFloor());
        dto.put("bookingTime", booking.getBookingTime().format(FORMATTER));
        dto.put("durationMinutes", Duration.between(booking.getBookingTime(), LocalDateTime.now()).toMinutes());
        
        return dto;
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
