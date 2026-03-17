package com.smartparking.dto;

import com.smartparking.enums.SlotStatus;

import java.time.LocalDateTime;

public class ParkingSlotDTO {
    
    private Long id;
    private Integer slotNumber;
    private Integer floor;
    private SlotStatus status;
    private LocalDateTime lockUntil;
    private Long remainingLockSeconds;
    
    public ParkingSlotDTO() {}
    
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
    
    public Long getRemainingLockSeconds() {
        return remainingLockSeconds;
    }
    
    public void setRemainingLockSeconds(Long remainingLockSeconds) {
        this.remainingLockSeconds = remainingLockSeconds;
    }
}
