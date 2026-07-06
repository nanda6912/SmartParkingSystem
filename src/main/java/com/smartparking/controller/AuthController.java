package com.smartparking.controller;

import com.smartparking.dto.LoginRequest;
import com.smartparking.dto.LoginResponse;
import com.smartparking.entity.StaffUser;
import com.smartparking.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Optional;

/**
 * Controller for backend authentication endpoints.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Authenticates user and establishes HTTP session.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        try {
            StaffUser authenticatedUser = authService.authenticate(
                loginRequest.getUsername(),
                loginRequest.getPassword()
            );

            // Establish the session
            authService.login(request, authenticatedUser);

            LoginResponse response = new LoginResponse(
                true,
                authenticatedUser.getUsername(),
                authenticatedUser.getFullName(),
                authenticatedUser.getRole()
            );
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            LoginResponse response = new LoginResponse(false, e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        } catch (Exception e) {
            LoginResponse response = new LoginResponse(false, "An internal server error occurred.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Invalidate the current session.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        authService.logout(request);
        return ResponseEntity.ok().build();
    }

    /**
     * Returns the currently authenticated user session details.
     */
    @GetMapping("/me")
    public ResponseEntity<LoginResponse> me(HttpServletRequest request) {
        Optional<StaffUser> currentUser = authService.getCurrentUser(request);
        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        StaffUser user = currentUser.get();
        LoginResponse response = new LoginResponse(
            true,
            user.getUsername(),
            user.getFullName(),
            user.getRole()
        );
        return ResponseEntity.ok(response);
    }
}
