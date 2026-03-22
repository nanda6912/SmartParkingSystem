package com.smartparking.controller;

import com.smartparking.dto.LoginRequestDTO;
import com.smartparking.entity.User;
import com.smartparking.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Controller for authentication endpoints
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "${app.cors.allowed-origins:http://localhost:8081}")
public class AuthController {
    
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    
    @Autowired
    private UserRepository userRepository;
    
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    /**
     * Authentication endpoint using database-backed validation
     * Validates credentials against the users table with BCrypt password hashing
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequestDTO loginRequest) {
        log.info("Authentication attempt for user: {} with role: {}", loginRequest.getUsername(), loginRequest.getRole());
        
        try {
            // Validate against database
            Optional<User> userOpt = userRepository.findByUsernameAndIsActiveTrue(loginRequest.getUsername());
            
            if (userOpt.isEmpty()) {
                log.warn("Authentication failed - user not found or inactive: {}", loginRequest.getUsername());
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("error", "Invalid credentials");
                response.put("message", "Username or password is incorrect");
                return ResponseEntity.badRequest().body(response);
            }
            
            User user = userOpt.get();
            
            // Validate password using BCrypt
            if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
                log.warn("Authentication failed - invalid password for user: {}", loginRequest.getUsername());
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("error", "Invalid credentials");
                response.put("message", "Username or password is incorrect");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Validate role matches
            if (loginRequest.getRole() == null || user.getRole() == null || !user.getRole().name().equalsIgnoreCase(loginRequest.getRole())) {
                log.warn("Authentication failed - role mismatch for user: {}. Expected: {}, Got: {}", 
                        loginRequest.getUsername(), user.getRole(), loginRequest.getRole());
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("error", "Invalid role");
                response.put("message", "User does not have the required role");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Update last login time
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);
            
            Map<String, Object> userData = new HashMap<>();
            userData.put("username", user.getUsername());
            userData.put("role", user.getRole().name());
            userData.put("name", user.getFullName());
            userData.put("authenticated", true);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("user", userData);
            response.put("redirectUrl", getRedirectUrl(loginRequest.getRole()));
            response.put("message", "Authentication successful");
            
            log.info("Authentication successful for user: {}", loginRequest.getUsername());
            return ResponseEntity.ok(response);
            
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
     * Get redirect URL based on role
     */
    private String getRedirectUrl(String role) {
        switch (role.toLowerCase()) {
            case "exit":
                return "/exit.html";
            case "operator":
                return "/index.html";
            default:
                return "/auth.html";
        }
    }
}
