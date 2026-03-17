package com.smartparking.entity;

import com.smartparking.enums.VehicleType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings", 
       uniqueConstraints = @UniqueConstraint(columnNames = "vehicle_number"))
public class Booking {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Booking code is required")
    @Column(name = "booking_code", nullable = true, unique = true, updatable = false, length = 5)
    private String bookingCode;
    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parking_slot_id", nullable = false)
    private ParkingSlot parkingSlot;
    
    @NotBlank(message = "Vehicle number is required")
    @Pattern(regexp = "^[A-Z]{2}[0-9]{2}[A-Z]{2}[0-9]{4}$", 
             message = "Vehicle number must be in format: XX00XX0000 (uppercase)")
    @Column(name = "vehicle_number", nullable = false, unique = true, updatable = false)
    private String vehicleNumber;
    
    @NotBlank(message = "Name is required")
    @Size(max = 20, message = "Name must be maximum 20 characters")
    @Pattern(regexp = "^[A-Za-z\\s]+$", message = "Name must contain only alphabets")
    @Column(nullable = false)
    private String customerName;
    
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must be exactly 10 digits")
    @Column(nullable = false)
    private String phoneNumber;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleType vehicleType;
    
    @NotNull
    @Column(name = "booking_time", nullable = false)
    private LocalDateTime bookingTime;
    
    @Column(name = "exit_time")
    private LocalDateTime exitTime;
    
    @Column(name = "parking_fee")
    private Integer parkingFee;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Version
    private Long version;
    
    public Booking() {
        this.bookingTime = LocalDateTime.now();
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getBookingCode() {
        return bookingCode;
    }
    
    public void setBookingCode(String bookingCode) {
        this.bookingCode = bookingCode;
    }
    
    public ParkingSlot getParkingSlot() {
        return parkingSlot;
    }
    
    public void setParkingSlot(ParkingSlot parkingSlot) {
        this.parkingSlot = parkingSlot;
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
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public Long getVersion() {
        return version;
    }
    
    public void setVersion(Long version) {
        this.version = version;
    }
}
