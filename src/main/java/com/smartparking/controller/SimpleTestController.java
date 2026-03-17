package com.smartparking.controller;

import com.smartparking.dto.ParkingSlotDTO;
import com.smartparking.enums.SlotStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/simple")
public class SimpleTestController {
    
    @GetMapping("/dto")
    public ParkingSlotDTO testDTO() {
        ParkingSlotDTO dto = new ParkingSlotDTO();
        dto.setId(1L);
        dto.setSlotNumber(1);
        dto.setFloor(1);
        dto.setStatus(SlotStatus.AVAILABLE);
        return dto;
    }
    
    @GetMapping("/enum")
    public SlotStatus testEnum() {
        return SlotStatus.AVAILABLE;
    }
}
