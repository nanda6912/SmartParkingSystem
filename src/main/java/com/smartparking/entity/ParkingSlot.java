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
        this.slotNumber = slotNumber;
        this.floor = floor;
        this.status = SlotStatus.AVAILABLE;
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
    }
    
    public Integer getFloor() {
        return floor;
    }
    
    public void setFloor(Integer floor) {
        this.floor = floor;
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
