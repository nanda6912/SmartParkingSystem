package com.smartparking.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.smartparking.enums.VehicleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class BookingRequestDTO {
    
    // Default constructor for JSON deserialization
    public BookingRequestDTO() {
    }
    
    // Custom creator for JSON deserialization
    @JsonCreator
    public BookingRequestDTO(
        @JsonProperty("slotId") Long slotId,
        @JsonProperty("vehicleNumber") String vehicleNumber,
        @JsonProperty("customerName") String customerName,
        @JsonProperty("phoneNumber") String phoneNumber,
        @JsonProperty("vehicleType") String vehicleType
    ) {
        this.slotId = slotId;
        this.vehicleNumber = vehicleNumber;
        this.customerName = customerName;
        this.phoneNumber = phoneNumber;
        // Handle vehicle type conversion
        if (vehicleType != null && !vehicleType.trim().isEmpty()) {
            try {
                this.vehicleType = VehicleType.valueOf(vehicleType.toUpperCase().trim());
            } catch (IllegalArgumentException e) {
                this.vehicleType = VehicleType.CAR; // Default fallback
            }
        } else {
            this.vehicleType = VehicleType.CAR; // Default fallback
        }
    }
    
    @NotNull(message = "Slot ID is required")
    private Long slotId;
    
    @NotBlank(message = "Vehicle number is required")
    @Pattern(regexp = "^[A-Z]{2}[0-9]{2}[A-Z]{2}[0-9]{4}$", 
             message = "Vehicle number must be in format: XX00XX0000 (uppercase)")
    private String vehicleNumber;
    
    @NotBlank(message = "Name is required")
    @Size(max = 20, message = "Name must be maximum 20 characters")
    @Pattern(regexp = "^[A-Za-z\\s]+$", message = "Name must contain only alphabets")
    private String customerName;
    
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must be exactly 10 digits")
    private String phoneNumber;
    
    @NotNull(message = "Vehicle type is required")
    private VehicleType vehicleType;
    
    public Long getSlotId() {
        return slotId;
    }
    
    public void setSlotId(Long slotId) {
        this.slotId = slotId;
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
    
    // Custom setter for JSON deserialization with error handling
    public void setVehicleType(String vehicleType) {
        if (vehicleType == null || vehicleType.trim().isEmpty()) {
            this.vehicleType = VehicleType.CAR; // Default fallback
        } else {
            try {
                this.vehicleType = VehicleType.valueOf(vehicleType.toUpperCase().trim());
            } catch (IllegalArgumentException e) {
                this.vehicleType = VehicleType.CAR; // Default fallback for invalid values
            }
        }
    }
}
