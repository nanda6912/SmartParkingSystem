package com.smartparking.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import jakarta.annotation.PostConstruct;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * Authentication Controller
 * Provides server-side authentication for exit staff and admin users
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:8081")
public class AuthController {
    
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Value("${auth.exit.user}")
    private String authExitUser;
    
    @Value("${auth.exit.password}")
    private String authExitPass;
    
    @Value("${auth.admin.user}")
    private String authAdminUser;
    
    @Value("${auth.admin.password}")
    private String authAdminPass;
    
    @PostConstruct
    public void validateConfiguration() {
        if (authExitUser == null || authExitUser.trim().isEmpty() ||
            authExitPass == null || authExitPass.trim().isEmpty() ||
            authAdminUser == null || authAdminUser.trim().isEmpty() ||
            authAdminPass == null || authAdminPass.trim().isEmpty()) {
            throw new IllegalStateException("Authentication configuration is missing. Please set auth.exit.user, auth.exit.password, auth.admin.user, and auth.admin.password in your configuration.");
        }
        log.info("Authentication configuration validated successfully");
    }
    
    /**
     * Authenticate user and return session info
     * For demo purposes, using hardcoded credentials
     * In production, this should validate against a database
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            // Validate credentials and get authoritative role from server
            String authoritativeRole = getRoleForUser(loginRequest.getUsername());
            if (authoritativeRole == null || !isValidCredentials(loginRequest.getUsername(), loginRequest.getPassword(), authoritativeRole)) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Invalid credentials"
                ));
            }
            
            // Create authentication token with server-determined role
            List<GrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority("ROLE_" + authoritativeRole.toUpperCase()));
            
            UsernamePasswordAuthenticationToken authToken = 
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getUsername(),
                    null,
                    authorities
                );
            
            // Set authentication context
            SecurityContextHolder.getContext().setAuthentication(authToken);
            
            // Return success response with server-determined role
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Authentication successful");
            response.put("username", loginRequest.getUsername());
            response.put("role", authoritativeRole); // Use server-determined role
            response.put("timestamp", java.time.LocalDateTime.now().toString());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Authentication error in AuthController.login", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Authentication failed"
            ));
        }
    }
    
    /**
     * Validate credentials (demo implementation)
     * In production, this should check against a user database
     */
    private boolean isValidCredentials(String username, String password, String role) {
        // Handle null or empty role up front
        if (role == null || role.trim().isEmpty()) {
            return false;
        }
        
        // Use externalized configuration with constant-time comparison
        switch (role.toLowerCase()) {
            case "exit":
                return constantTimeEquals(authExitUser, username) && constantTimeEquals(authExitPass, password);
            case "admin":
                return constantTimeEquals(authAdminUser, username) && constantTimeEquals(authAdminPass, password);
            default:
                return false;
        }
    }
    
    /**
     * Constant-time comparison to prevent timing attacks
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return a == b;
        }
        
        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);
        
        return MessageDigest.isEqual(aBytes, bBytes);
    }
    
    /**
     * Get authoritative role for a username (server-side determination)
     * In production, this should check against a user database or directory service
     */
    private String getRoleForUser(String username) {
        // For demo purposes, determine role based on username
        // In production, this should query a database or user directory
        if (authExitUser.equals(username)) {
            return "exit";
        } else if (authAdminUser.equals(username)) {
            return "admin";
        }
        return null; // Unknown user
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
        
        // Reject anonymous principals
        if (auth == null || auth instanceof AnonymousAuthenticationToken || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.ok(Map.of(
                "authenticated", false
            ));
        }
        
        // Extract username and role safely
        String username = auth.getName();
        String role = null;
        
        // Get role from authorities
        if (!auth.getAuthorities().isEmpty()) {
            role = auth.getAuthorities().iterator().next().getAuthority();
            // Remove ROLE_ prefix if present
            if (role != null && role.startsWith("ROLE_")) {
                role = role.substring(5);
            }
        }
        
        return ResponseEntity.ok(Map.of(
            "authenticated", true,
            "username", username,
            "role", role != null ? role.toLowerCase() : "unknown"
        ));
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
