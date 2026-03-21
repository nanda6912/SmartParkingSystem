package com.smartparking.controller;

import com.smartparking.dto.LoginRequestDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for authentication endpoints
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "${app.cors.allowed-origins:http://localhost:8081}")
public class AuthController {
    
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    
    /**
     * Simple authentication endpoint for exit management
     * In a real application, this would validate against a database
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequestDTO loginRequest) {
        log.info("Authentication attempt for user: {} with role: {}", loginRequest.getUsername(), loginRequest.getRole());
        
        try {
            // Simple authentication logic (for demo purposes)
            // In production, validate against database with proper password hashing
            if (isValidCredentials(loginRequest)) {
                Map<String, Object> user = new HashMap<>();
                user.put("username", loginRequest.getUsername());
                user.put("role", loginRequest.getRole());
                user.put("name", getDisplayName(loginRequest.getUsername()));
                user.put("authenticated", true);
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("user", user);
                response.put("redirectUrl", getRedirectUrl(loginRequest.getRole()));
                response.put("message", "Authentication successful");
                
                log.info("Authentication successful for user: {}", loginRequest.getUsername());
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("error", "Invalid credentials");
                response.put("message", "Username or password is incorrect");
                
                log.warn("Authentication failed for user: {}", loginRequest.getUsername());
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            log.error("Authentication error for user: {}", loginRequest.getUsername(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "Authentication failed");
            response.put("message", "An error occurred during authentication");
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Logout endpoint
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        log.info("Logout request for user: {}", username);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Logged out successfully");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Simple credential validation (for demo purposes)
     * In production, use proper password hashing and database validation
     */
    private boolean isValidCredentials(LoginRequestDTO loginRequest) {
        String username = loginRequest.getUsername();
        String password = loginRequest.getPassword();
        String role = loginRequest.getRole();
        
        // Demo credentials - in production, validate against database
        switch (role.toLowerCase()) {
            case "exit":
                return "exit".equals(username) && "exit123".equals(password);
            case "operator":
                return "operator".equals(username) && "operator123".equals(password);
            default:
                return false;
        }
    }
    
    /**
     * Get display name for user
     */
    private String getDisplayName(String username) {
        switch (username.toLowerCase()) {
            case "exit":
                return "Exit Operator";
            case "operator":
                return "Parking Operator";
            default:
                return "Unknown User";
        }
    }
    
    /**
     * Get redirect URL based on role
     */
    private String getRedirectUrl(String role) {
        switch (role.toLowerCase()) {
            case "exit":
                return "/exit.html";
            case "operator":
                return "/booking.html";
            default:
                return "/auth.html";
        }
    }
}
