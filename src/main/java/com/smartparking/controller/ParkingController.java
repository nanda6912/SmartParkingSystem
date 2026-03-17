package com.smartparking.controller;

import com.smartparking.dto.BookingRequestDTO;
import com.smartparking.dto.BookingResponseDTO;
import com.smartparking.dto.ParkingSlotDTO;
import com.smartparking.service.ParkingService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/parking-slots")
@Validated
@CrossOrigin(origins = "*")
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
    public ResponseEntity<BookingResponseDTO> bookSlot(@Valid @RequestBody BookingRequestDTO bookingRequest) {
        BookingResponseDTO response = parkingService.bookSlot(bookingRequest);
        return ResponseEntity.ok(response);
    }
}
