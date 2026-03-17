package com.smartparking.dto;

import com.smartparking.enums.SlotStatus;
import com.smartparking.enums.VehicleType;

import java.time.LocalDateTime;

public class BookingResponseDTO {
    
    private String bookingCode;
    private Long slotId;
    private Integer slotNumber;
    private Integer floor;
    private String vehicleNumber;
    private String customerName;
    private String phoneNumber;
    private VehicleType vehicleType;
    private LocalDateTime bookingTime;
    private LocalDateTime exitTime;
    private Integer parkingFee;
    private SlotStatus slotStatus;
    private String message;
    
    public BookingResponseDTO() {}
    
    public BookingResponseDTO(String message) {
        this.message = message;
    }
    
    public String getBookingCode() {
        return bookingCode;
    }
    
    public void setBookingCode(String bookingCode) {
        this.bookingCode = bookingCode;
    }
    
    public Long getSlotId() {
        return slotId;
    }
    
    public void setSlotId(Long slotId) {
        this.slotId = slotId;
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
    
    public String getVehicleNumber() {
        return vehicleNumber;
    }
    
    public void setVehicleNumber(String vehicleNumber) {
        this.vehicleNumber = vehicleNumber;
    }
    
    public String getCustomerName() {
        return customerName;
    }
    
    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }
    
    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    
    public VehicleType getVehicleType() {
        return vehicleType;
    }
    
    public void setVehicleType(VehicleType vehicleType) {
        this.vehicleType = vehicleType;
    }
    
    public LocalDateTime getBookingTime() {
        return bookingTime;
    }
    
    public void setBookingTime(LocalDateTime bookingTime) {
        this.bookingTime = bookingTime;
    }
    
    public LocalDateTime getExitTime() {
        return exitTime;
    }
    
    public void setExitTime(LocalDateTime exitTime) {
        this.exitTime = exitTime;
    }
    
    public Integer getParkingFee() {
        return parkingFee;
    }
    
    public void setParkingFee(Integer parkingFee) {
        this.parkingFee = parkingFee;
    }
    
    public SlotStatus getSlotStatus() {
        return slotStatus;
    }
    
    public void setSlotStatus(SlotStatus slotStatus) {
        this.slotStatus = slotStatus;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}
