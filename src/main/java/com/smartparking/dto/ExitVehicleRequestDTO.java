package com.smartparking.dto;

import jakarta.validation.constraints.NotBlank;

public class ExitVehicleRequestDTO {
    
    @NotBlank(message = "Vehicle number is required")
    private String vehicleNumber;
    
    public ExitVehicleRequestDTO() {}
    
    public ExitVehicleRequestDTO(String vehicleNumber) {
        this.vehicleNumber = vehicleNumber;
    }
    
    public String getVehicleNumber() {
        return vehicleNumber;
    }
    
    public void setVehicleNumber(String vehicleNumber) {
        this.vehicleNumber = vehicleNumber;
    }
}
