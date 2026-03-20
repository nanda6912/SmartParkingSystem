package com.smartparking.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")
public class HealthController {
    
    private static final Logger log = LoggerFactory.getLogger(HealthController.class);
    
    @GetMapping
    public String health() {
        log.debug("Health check requested");
        return "Smart Parking System is running!";
    }
}
