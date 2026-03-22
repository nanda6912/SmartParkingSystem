package com.smartparking.service;

import com.smartparking.dto.BookingRequestDTO;
import com.smartparking.dto.BookingResponseDTO;
import com.smartparking.dto.ParkingSlotDTO;
import com.smartparking.entity.Booking;
import com.smartparking.entity.ParkingSlot;
import com.smartparking.enums.SlotStatus;
import com.smartparking.repository.BookingRepository;
import com.smartparking.repository.ParkingSlotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class ParkingService {
    
    private static final Logger log = LoggerFactory.getLogger(ParkingService.class);
    
    @Autowired
    private ParkingSlotRepository parkingSlotRepository;
    
    @Autowired
    private BookingRepository bookingRepository;
    
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int BOOKING_CODE_LENGTH = 5;
    private final Random random = new Random();
    
    private String generateBookingCode() {
        StringBuilder code = new StringBuilder(BOOKING_CODE_LENGTH);
        for (int i = 0; i < BOOKING_CODE_LENGTH; i++) {
            code.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return code.toString();
    }
    
    public List<ParkingSlotDTO> getAllSlots() {
        try {
            List<ParkingSlot> slots = parkingSlotRepository.findAll();
            log.debug("Found {} slots", slots.size());
            return slots.stream()
                    .sorted((a, b) -> {
                        // Sort by floor first, then by slot number
                        int floorCompare = Integer.compare(a.getFloor(), b.getFloor());
                        if (floorCompare != 0) {
                            return floorCompare;
                        }
                        return Integer.compare(a.getSlotNumber(), b.getSlotNumber());
                    })
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error in getAllSlots: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    public List<ParkingSlotDTO> getSlotsByFloor(Integer floor) {
        List<ParkingSlot> slots = parkingSlotRepository.findByFloor(floor);
        return slots.stream()
                .sorted((a, b) -> Integer.compare(a.getSlotNumber(), b.getSlotNumber()))
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public BookingResponseDTO lockSlot(Long slotId) {
        try {
            Optional<ParkingSlot> slotOpt = parkingSlotRepository.findByIdWithLock(slotId);
            if (slotOpt.isEmpty()) {
                return new BookingResponseDTO("Slot not found");
            }
            
            ParkingSlot slot = slotOpt.get();
            
            if (slot.getStatus() != SlotStatus.AVAILABLE) {
                return new BookingResponseDTO("Slot is not available");
            }
            
            slot.setStatus(SlotStatus.LOCKED);
            slot.setLockUntil(LocalDateTime.now().plusMinutes(5)); // Increased from 2 to 5 minutes
            parkingSlotRepository.save(slot);
            
            ParkingSlotDTO slotDTO = convertToDTO(slot);
            BookingResponseDTO response = new BookingResponseDTO("Slot locked successfully");
            response.setSlotId(slotId);
            response.setSlotStatus(SlotStatus.LOCKED);
            
            return response;
            
        } catch (Exception e) {
            return new BookingResponseDTO("Error locking slot: " + e.getMessage());
        }
    }
    
    public BookingResponseDTO bookSlot(BookingRequestDTO bookingRequest) {
        try {
            log.debug("=== BOOKING REQUEST DEBUG ===");
            log.debug("Slot ID: {}", bookingRequest.getSlotId());
            log.debug("Vehicle Number: {}", bookingRequest.getVehicleNumber());
            log.debug("Customer Name: {}", bookingRequest.getCustomerName());
            log.debug("Phone Number: {}", bookingRequest.getPhoneNumber());
            log.debug("Vehicle Type: {}", bookingRequest.getVehicleType());
            
            Optional<ParkingSlot> slotOpt = parkingSlotRepository.findByIdWithLock(bookingRequest.getSlotId());
            if (slotOpt.isEmpty()) {
                log.warn("Slot not found for ID: {}", bookingRequest.getSlotId());
                return new BookingResponseDTO("Slot not found");
            }
            
            ParkingSlot slot = slotOpt.get();
            log.debug("Slot found - ID: {}, Status: {}, Locked until: {}", 
                     slot.getId(), slot.getStatus(), slot.getLockUntil());
            
            if (slot.getStatus() != SlotStatus.LOCKED) {
                log.warn("Slot {} is not locked - Current status: {}", slot.getId(), slot.getStatus());
                return new BookingResponseDTO("Slot is not locked. Please lock the slot first.");
            }
            
            if (slot.getLockUntil() != null && slot.getLockUntil().isBefore(LocalDateTime.now())) {
                log.info("Lock expired for slot {}", slot.getId());
                slot.setStatus(SlotStatus.AVAILABLE);
                slot.setLockUntil(null);
                parkingSlotRepository.save(slot);
                return new BookingResponseDTO("Lock expired, slot is now available");
            }
            
            // Check if vehicle is currently parked (only active bookings, not exited ones)
            Optional<Booking> existingActiveBooking = bookingRepository.findByVehicleNumberAndIsActiveTrue(bookingRequest.getVehicleNumber().toUpperCase());
            log.debug("Checking vehicle: {}", bookingRequest.getVehicleNumber().toUpperCase());
            log.debug("Existing active booking found: {}", existingActiveBooking.isPresent());
            
            if (existingActiveBooking.isPresent()) {
                log.warn("Vehicle {} is currently parked, blocking new booking", bookingRequest.getVehicleNumber());
                return new BookingResponseDTO("Vehicle is currently parked");
            }
            
            Booking booking = new Booking();
            booking.setBookingCode(generateBookingCode());
            booking.setParkingSlot(slot);
            booking.setVehicleNumber(bookingRequest.getVehicleNumber().toUpperCase());
            booking.setCustomerName(bookingRequest.getCustomerName());
            booking.setPhoneNumber(bookingRequest.getPhoneNumber());
            booking.setVehicleType(bookingRequest.getVehicleType());
            booking.setBookingTime(LocalDateTime.now());
            
            Booking savedBooking = bookingRepository.save(booking);
            
            slot.setStatus(SlotStatus.OCCUPIED);
            slot.setLockUntil(null);
            parkingSlotRepository.save(slot);
            
            return convertToResponseDTO(savedBooking);
            
        } catch (Exception e) {
            return new BookingResponseDTO("Error booking slot: " + e.getMessage());
        }
    }
    
    @Transactional
    public void releaseExpiredLocks() {
        List<ParkingSlot> expiredLockedSlots = parkingSlotRepository.findExpiredLockedSlots(LocalDateTime.now());
        for (ParkingSlot slot : expiredLockedSlots) {
            slot.setStatus(SlotStatus.AVAILABLE);
            slot.setLockUntil(null);
            parkingSlotRepository.save(slot);
        }
    }
    
    private ParkingSlotDTO convertToDTO(ParkingSlot slot) {
        if (slot == null) {
            return null;
        }
        ParkingSlotDTO dto = new ParkingSlotDTO();
        dto.setId(slot.getId());
        dto.setSlotNumber(slot.getSlotNumber());
        dto.setFloor(slot.getFloor());
        dto.setSlotId(slot.getSlotId() != null ? slot.getSlotId() : "UNKNOWN");
        dto.setStatus(slot.getStatus());
        
        // Defensive null handling for lockUntil
        dto.setLockUntil(slot.getLockUntil());
        
        if (slot.getLockUntil() != null && slot.getStatus() == SlotStatus.LOCKED) {
            try {
                long remainingSeconds = java.time.Duration.between(LocalDateTime.now(), slot.getLockUntil()).getSeconds();
                dto.setRemainingLockSeconds(Math.max(0, remainingSeconds));
            } catch (Exception e) {
                log.warn("Error calculating remaining lock seconds for slot {}: {}", slot.getId(), e.getMessage());
                dto.setRemainingLockSeconds(0L);
            }
        }
        
        return dto;
    }
    
    private BookingResponseDTO convertToResponseDTO(Booking booking) {
        if (booking == null || booking.getParkingSlot() == null) {
            log.error("Cannot convert null booking or booking with null parking slot to DTO");
            BookingResponseDTO errorDto = new BookingResponseDTO("Invalid booking data");
            errorDto.setSlotStatus(SlotStatus.AVAILABLE);
            errorDto.setBookingCode("ERROR");
            errorDto.setSlotId(0L);
            errorDto.setSlotIdString("UNKNOWN");
            errorDto.setSlotNumber(0);
            errorDto.setFloor(0);
            return errorDto;
        }
        
        BookingResponseDTO dto = new BookingResponseDTO();
        dto.setBookingCode(booking.getBookingCode());
        dto.setSlotId(booking.getParkingSlot().getId());
        dto.setSlotIdString(booking.getParkingSlot().getSlotId() != null ? 
                booking.getParkingSlot().getSlotId() : "UNKNOWN");
        dto.setSlotNumber(booking.getParkingSlot().getSlotNumber());
        dto.setFloor(booking.getParkingSlot().getFloor());
        dto.setVehicleNumber(booking.getVehicleNumber());
        dto.setCustomerName(booking.getCustomerName());
        dto.setPhoneNumber(booking.getPhoneNumber());
        dto.setVehicleType(booking.getVehicleType());
        dto.setBookingTime(booking.getBookingTime());
        dto.setExitTime(booking.getExitTime());
        dto.setParkingFee(booking.getParkingFee());
        dto.setSlotStatus(booking.getParkingSlot().getStatus());
        return dto;
    }
}
