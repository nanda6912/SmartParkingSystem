package com.smartparking.service;

import com.smartparking.entity.StaffUser;
import com.smartparking.repository.StaffUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Optional;

/**
 * Service for staff authentication and session management.
 */
@Service
public class AuthService {

    public static final String SESSION_USER_KEY = "authenticatedUser";

    private final StaffUserRepository staffUserRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(StaffUserRepository staffUserRepository, PasswordEncoder passwordEncoder) {
        this.staffUserRepository = staffUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Authenticates a user by username and password.
     * Checks if user is enabled.
     */
    public StaffUser authenticate(String username, String password) {
        Optional<StaffUser> oUser = staffUserRepository.findByUsername(username);
        if (oUser.isEmpty()) {
            throw new IllegalArgumentException("Invalid username or password.");
        }
        StaffUser user = oUser.get();
        if (Boolean.FALSE.equals(user.getEnabled())) {
            throw new IllegalArgumentException("Account is disabled.");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Invalid username or password.");
        }
        return user;
    }

    /**
     * Logs the user in by creating a new session (with session fixation protection).
     */
    public void login(HttpServletRequest request, StaffUser user) {
        // Invalidate old session to prevent session fixation attacks
        HttpSession oldSession = request.getSession(false);
        if (oldSession != null) {
            oldSession.invalidate();
        }
        // Create new session and store user object
        HttpSession newSession = request.getSession(true);
        newSession.setAttribute(SESSION_USER_KEY, user);
    }

    /**
     * Logs the user out by invalidating the current session.
     */
    public void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    /**
     * Retrieves the current authenticated user from the session, if present.
     */
    public Optional<StaffUser> getCurrentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return Optional.empty();
        }
        StaffUser user = (StaffUser) session.getAttribute(SESSION_USER_KEY);
        return Optional.ofNullable(user);
    }
}
