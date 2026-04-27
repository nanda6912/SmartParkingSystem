package com.smartparking.config;

import com.smartparking.entity.ParkingSlot;
import com.smartparking.repository.ParkingSlotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private ParkingSlotRepository parkingSlotRepository;

    @Override
    @Transactional
    public void run(String... args) {
        long count = parkingSlotRepository.count();
        
        if (count == 0) {
            log.info("No parking slots found. Initializing 600 slots...");
            
            // Floor 1: 5 areas (A-E), 60 slots each = 300 slots
            // Floor 2: 5 areas (A-E), 60 slots each = 300 slots
            for (int floor = 1; floor <= 2; floor++) {
                for (int slotNum = 1; slotNum <= 300; slotNum++) {
                    ParkingSlot slot = new ParkingSlot(slotNum, floor);
                    parkingSlotRepository.save(slot);
                }
            }
            
            log.info("Initialized 600 parking slots successfully");
        } else {
            log.info("Found {} existing parking slots. Skipping initialization.", count);
        }
    }
}
