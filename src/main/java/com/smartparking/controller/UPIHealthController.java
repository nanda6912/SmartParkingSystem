package com.smartparking.controller;

import com.smartparking.service.UPIPaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Health check controller for UPI payment configuration
 */
@RestController
@RequestMapping("/api/health")
public class UPIHealthController {
    
    private static final Logger log = LoggerFactory.getLogger(UPIHealthController.class);
    
    @Autowired
    private UPIPaymentService upiPaymentService;
    
    /**
     * Check UPI configuration health
     */
    @GetMapping("/upi")
    public ResponseEntity<Map<String, Object>> checkUPIHealth() {
        try {
            Map<String, Object> status = upiPaymentService.getUPIConfigurationStatus();
            
            if ((Boolean) status.get("configured")) {
                return ResponseEntity.ok(status);
            } else {
                return ResponseEntity.badRequest().body(status);
            }
            
        } catch (Exception e) {
            log.error("Error checking UPI health: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to check UPI configuration", 
                              "message", e.getMessage()));
        }
    }
}
