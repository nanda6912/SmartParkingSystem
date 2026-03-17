package com.smartparking.controller;

import com.smartparking.entity.ParkingSlot;
import com.smartparking.repository.ParkingSlotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/debug")
public class DebugController {
    
    @Autowired
    private ParkingSlotRepository parkingSlotRepository;
    
    @GetMapping("/count")
    public String testCount() {
        try {
            long count = parkingSlotRepository.count();
            return "Count successful: " + count;
        } catch (Exception e) {
            return "Count failed: " + e.getMessage();
        }
    }
    
    @GetMapping("/first")
    public String testFirst() {
        try {
            Optional<ParkingSlot> slot = parkingSlotRepository.findById(1L);
            if (slot.isPresent()) {
                return "First slot found: ID=" + slot.get().getId() + 
                       ", Floor=" + slot.get().getFloor() + 
                       ", Status=" + slot.get().getStatus() +
                       ", LockUntil=" + slot.get().getLockUntil();
            } else {
                return "No slot found with ID 1";
            }
        } catch (Exception e) {
            return "First slot failed: " + e.getMessage();
        }
    }
    
    @GetMapping("/list")
    public String testList() {
        try {
            List<ParkingSlot> slots = parkingSlotRepository.findAll();
            return "List successful: Found " + slots.size() + " slots";
        } catch (Exception e) {
            return "List failed: " + e.getMessage();
        }
    }
}
