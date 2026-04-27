package com.smartparking.entity;

import com.smartparking.enums.SlotStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "parking_slots")
public class ParkingSlot {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull
    @Min(1)
    @Column(name = "slot_number", nullable = false)
    private Integer slotNumber;
    
    @NotNull
    @Min(1)
    @Column(name = "floor", nullable = false)
    private Integer floor;
    
    @Column(name = "slot_id", nullable = false, unique = true)
    private String slotId;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SlotStatus status;
    
    @Column(name = "lock_until", nullable = true)
    private LocalDateTime lockUntil;
    
    @Version
    @Column(name = "version")
    private Long version;
    
    public ParkingSlot() {
        this.status = SlotStatus.AVAILABLE;
    }
    
    public ParkingSlot(Integer slotNumber, Integer floor) {
        if (slotNumber == null || floor == null) {
            throw new IllegalArgumentException("slotNumber and floor cannot be null");
        }
        if (slotNumber < 1 || slotNumber > 300) {
            throw new IllegalArgumentException("slotNumber must be between 1 and 300");
        }
        if (floor < 1 || floor > 2) {
            throw new IllegalArgumentException("floor must be 1 or 2");
        }
        this.slotNumber = slotNumber;
        this.floor = floor;
        this.slotId = generateSlotId(floor, slotNumber);
        this.status = SlotStatus.AVAILABLE;
    }
    
    private String generateSlotId(Integer floor, Integer slotNumber) {
        if (floor == null || slotNumber == null) {
            throw new IllegalArgumentException("floor and slotNumber cannot be null");
        }
        // Generate floor-based alphabetic groups
        // Floor 1: AG01-AG60, BG01-BG60, CG01-CG60, DG01-DG60, EG01-EG60
        // Floor 2: AF01-AF60, BF01-BF60, CF01-CF60, DF01-DF60, EF01-EF60
        char groupLetter = (char) ('A' + ((slotNumber - 1) / 60));
        int slotInGroup = ((slotNumber - 1) % 60) + 1;
        char floorLetter = (floor == 1) ? 'G' : 'F';
        
        return String.format("%c%c%02d", groupLetter, floorLetter, slotInGroup);
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Integer getSlotNumber() {
        return slotNumber;
    }
    
    public void setSlotNumber(Integer slotNumber) {
        this.slotNumber = slotNumber;
        // Regenerate slotId when slot number changes (if floor is also set)
        if (this.floor != null && slotNumber != null) {
            this.slotId = generateSlotId(this.floor, slotNumber);
        }
    }
    
    public Integer getFloor() {
        return floor;
    }
    
    public void setFloor(Integer floor) {
        this.floor = floor;
        // Regenerate slotId when floor changes (if slot number is also set)
        if (this.slotNumber != null && floor != null) {
            this.slotId = generateSlotId(floor, this.slotNumber);
        }
    }
    
    public String getSlotId() {
        return slotId;
    }
    
    public void setSlotId(String slotId) {
        this.slotId = slotId;
    }
    
    public SlotStatus getStatus() {
        return status;
    }
    
    public void setStatus(SlotStatus status) {
        this.status = status;
    }
    
    public LocalDateTime getLockUntil() {
        return lockUntil;
    }
    
    public void setLockUntil(LocalDateTime lockUntil) {
        this.lockUntil = lockUntil;
    }
    
    public Long getVersion() {
        return version;
    }
    
    public void setVersion(Long version) {
        this.version = version;
    }
}
