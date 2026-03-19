package com.smartparking.controller;

import com.smartparking.dto.BookingRequestDTO;
import com.smartparking.dto.BookingResponseDTO;
import com.smartparking.dto.ParkingSlotDTO;
import com.smartparking.service.ParkingService;
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
    
    @Autowired
    private ParkingService parkingService;
    
    @GetMapping
    public ResponseEntity<List<ParkingSlotDTO>> getAllSlots() {
        List<ParkingSlotDTO> slots = parkingService.getAllSlots();
        return ResponseEntity.ok(slots);
    }
    
    @GetMapping("/floor/{floor}")
    public ResponseEntity<List<ParkingSlotDTO>> getSlotsByFloor(@PathVariable Integer floor) {
        List<ParkingSlotDTO> slots = parkingService.getSlotsByFloor(floor);
        return ResponseEntity.ok(slots);
    }
    
    @PostMapping("/lock/{slotId}")
    public ResponseEntity<BookingResponseDTO> lockSlot(@PathVariable Long slotId) {
        BookingResponseDTO response = parkingService.lockSlot(slotId);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/book")
    public ResponseEntity<BookingResponseDTO> bookSlot(@RequestBody BookingRequestDTO bookingRequest) {
        System.out.println("=== CONTROLLER DEBUG ===");
        System.out.println("Received booking request: " + bookingRequest);
        System.out.println("Slot ID: " + bookingRequest.getSlotId());
        System.out.println("Vehicle Number: " + bookingRequest.getVehicleNumber());
        System.out.println("Customer Name: " + bookingRequest.getCustomerName());
        System.out.println("Phone Number: " + bookingRequest.getPhoneNumber());
        System.out.println("Vehicle Type: " + bookingRequest.getVehicleType());
        
        BookingResponseDTO response = parkingService.bookSlot(bookingRequest);
        return ResponseEntity.ok(response);
    }
    
    // Debug endpoint to test raw request
    @PostMapping("/book-debug")
    public ResponseEntity<String> debugBook(@RequestBody String rawRequest) {
        System.out.println("=== RAW REQUEST DEBUG ===");
        System.out.println("Raw request: " + rawRequest);
        return ResponseEntity.ok("Request received: " + rawRequest);
    }
}
