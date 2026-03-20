package com.smartparking.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Authentication Controller
 * Provides server-side authentication for exit staff and admin users
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:8081")
public class AuthController {
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    /**
     * Authenticate user and return session info
     * For demo purposes, using hardcoded credentials
     * In production, this should validate against a database
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            // For demo purposes, validate hardcoded credentials
            if (!isValidCredentials(loginRequest.getUsername(), loginRequest.getPassword(), loginRequest.getRole())) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Invalid credentials"
                ));
            }
            
            // Create authentication token (for demo purposes)
            UsernamePasswordAuthenticationToken authToken = 
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getUsername() + ":" + loginRequest.getRole(),
                    null,
                    java.util.Collections.emptyList()
                );
            
            // Set authentication context
            SecurityContextHolder.getContext().setAuthentication(authToken);
            
            // Return success response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Authentication successful");
            response.put("username", loginRequest.getUsername());
            response.put("role", loginRequest.getRole());
            response.put("timestamp", java.time.LocalDateTime.now().toString());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Authentication failed: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Validate credentials (demo implementation)
     * In production, this should check against a user database
     */
    private boolean isValidCredentials(String username, String password, String role) {
        // Demo credentials - same as the removed auth-demo.html
        switch (role.toLowerCase()) {
            case "exit":
                return "exit".equals(username) && "exit456".equals(password);
            case "admin":
                return "admin".equals(username) && "admin789".equals(password);
            default:
                return false;
        }
    }
    
    /**
     * Logout endpoint
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Logged out successfully"
        ));
    }
    
    /**
     * Check authentication status
     */
    @GetMapping("/status")
    public ResponseEntity<?> checkAuthStatus() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth != null && auth.isAuthenticated()) {
            String[] userInfo = auth.getName().split(":");
            return ResponseEntity.ok(Map.of(
                "authenticated", true,
                "username", userInfo.length > 0 ? userInfo[0] : "unknown",
                "role", userInfo.length > 1 ? userInfo[1] : "unknown"
            ));
        } else {
            return ResponseEntity.ok(Map.of(
                "authenticated", false
            ));
        }
    }
    
    /**
     * Login request DTO
     */
    public static class LoginRequest {
        private String username;
        private String password;
        private String role;
        
        // Getters and setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
    }
}
