package com.smartparking.controller;

import com.smartparking.repository.ParkingSlotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class DatabaseTestController {
    
    @Autowired
    private ParkingSlotRepository parkingSlotRepository;
    
    @GetMapping("/db")
    public String testDatabase() {
        try {
            long count = parkingSlotRepository.count();
            return "Database connection successful! Total slots: " + count;
        } catch (Exception e) {
            return "Database connection failed: " + e.getMessage();
        }
    }
}
