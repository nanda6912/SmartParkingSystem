package com.smartparking.controller;

import com.smartparking.dto.BookingRequestDTO;
import com.smartparking.dto.BookingResponseDTO;
import com.smartparking.dto.ParkingSlotDTO;
import com.smartparking.service.ParkingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/parking-slots")
@Validated
@CrossOrigin(origins = "${app.cors.allowed-origins:http://localhost:8081}")
public class ParkingController {
    
    private static final Logger log = LoggerFactory.getLogger(ParkingController.class);
    
    @Autowired
    private ParkingService parkingService;
    
    @GetMapping
    public ResponseEntity<List<ParkingSlotDTO>> getAllSlots() {
        log.debug("Fetching all parking slots");
        List<ParkingSlotDTO> slots = parkingService.getAllSlots();
        log.debug("Retrieved {} parking slots", slots.size());
        return ResponseEntity.ok(slots);
    }
    
    @GetMapping("/floor/{floor}")
    public ResponseEntity<List<ParkingSlotDTO>> getSlotsByFloor(@PathVariable Integer floor) {
        log.debug("Fetching parking slots for floor: {}", floor);
        List<ParkingSlotDTO> slots = parkingService.getSlotsByFloor(floor);
        log.debug("Retrieved {} parking slots for floor {}", slots.size(), floor);
        return ResponseEntity.ok(slots);
    }
    
    @PostMapping("/lock/{slotId}")
    public ResponseEntity<BookingResponseDTO> lockSlot(@PathVariable Long slotId) {
        try {
            log.debug("Locking slot: {}", slotId);
            BookingResponseDTO response = parkingService.lockSlot(slotId);
            log.debug("Slot {} locked successfully", slotId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Business logic error locking slot {}: {}", slotId, e.getMessage());
            return ResponseEntity.badRequest().body(new BookingResponseDTO("Failed to lock slot: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error locking slot {}: {}", slotId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(new BookingResponseDTO("Internal server error while locking slot"));
        }
    }
    
    @PostMapping("/book")
    public ResponseEntity<BookingResponseDTO> bookSlot(@RequestBody BookingRequestDTO bookingRequest) {
        try {
            log.debug("Booking slot: {}", bookingRequest.getSlotId());
            log.debug("Vehicle number: {}", bookingRequest.getVehicleNumber());
            
            BookingResponseDTO response = parkingService.bookSlot(bookingRequest);
            
            if (response.getBookingCode() != null) {
                log.debug("Slot booked successfully with booking code: {}", response.getBookingCode());
                return ResponseEntity.ok(response);
            } else {
                log.warn("Booking failed for slot {}: {}", bookingRequest.getSlotId(), response.getMessage());
                return ResponseEntity.badRequest().body(response);
            }
        } catch (RuntimeException e) {
            log.error("Business logic error booking slot {}: {}", bookingRequest.getSlotId(), e.getMessage());
            return ResponseEntity.badRequest().body(new BookingResponseDTO("Failed to book slot: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error booking slot {}: {}", bookingRequest.getSlotId(), e.getMessage(), e);
            return ResponseEntity.internalServerError().body(new BookingResponseDTO("Internal server error while booking slot"));
        }
    }
    
    // Debug endpoint to test raw request
    @PostMapping("/book-debug")
    public ResponseEntity<String> debugBook(@RequestBody String rawRequest) {
        System.out.println("=== RAW REQUEST DEBUG ===");
        System.out.println("Raw request: " + rawRequest);
        return ResponseEntity.ok("Request received: " + rawRequest);
    }
}
