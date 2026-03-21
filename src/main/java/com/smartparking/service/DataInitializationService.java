package com.smartparking.service;

import com.smartparking.entity.ParkingSlot;
import com.smartparking.entity.User;
import com.smartparking.enums.SlotStatus;
import com.smartparking.enums.UserRole;
import com.smartparking.enums.VehicleType;
import com.smartparking.repository.ParkingSlotRepository;
import com.smartparking.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DataInitializationService implements CommandLineRunner {
    
    @Autowired
    private ParkingSlotRepository parkingSlotRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Override
    public void run(String... args) throws Exception {
        initializeParkingSlots();
        initializeUsers();
    }
    
    private void initializeParkingSlots() {
        if (parkingSlotRepository.count() == 0) {
            List<ParkingSlot> slots = new ArrayList<>();
            
            for (int floor = 1; floor <= 2; floor++) {
                for (int slotNumber = 1; slotNumber <= 100; slotNumber++) {
                    ParkingSlot slot = new ParkingSlot(slotNumber, floor);
                    slot.setStatus(SlotStatus.AVAILABLE);
                    slots.add(slot);
                }
            }
            
            parkingSlotRepository.saveAll(slots);
            System.out.println("Initialized 200 parking slots (100 per floor)");
        }
    }
    
    private void initializeUsers() {
        if (userRepository.count() == 0) {
            User staff = new User();
            staff.setUsername("staff");
            staff.setPassword(passwordEncoder.encode("staff123"));
            staff.setRole(UserRole.STAFF);
            staff.setFullName("Parking Staff");
            staff.setEmail("staff@smartparking.com");
            staff.setIsActive(true);
            
            userRepository.save(staff);
            
            System.out.println("Initialized default users:");
            System.out.println("Staff - Username: staff, Password: staff123");
        }
    }
}
