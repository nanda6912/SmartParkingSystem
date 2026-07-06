package com.smartparking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartparking.dto.LoginRequest;
import com.smartparking.entity.StaffUser;
import com.smartparking.service.AuthService;
import com.smartparking.service.RateLimitingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private RateLimitingService rateLimitingService;

    @Autowired
    private ObjectMapper objectMapper;

    private StaffUser testUser;

    @BeforeEach
    void setUp() {
        // Stub RateLimitingService to allow all request flows
        when(rateLimitingService.isLockAllowed(anyString())).thenReturn(true);
        when(rateLimitingService.isBookAllowed(anyString())).thenReturn(true);
        when(rateLimitingService.isReceiptAllowed(anyString())).thenReturn(true);
        when(rateLimitingService.isViewAllowed(anyString())).thenReturn(true);
        when(rateLimitingService.isGeneralAllowed(anyString())).thenReturn(true);

        testUser = new StaffUser();
        testUser.setId(1L);
        testUser.setUsername("testadmin");
        testUser.setPassword("hashedpassword");
        testUser.setFullName("Test Admin");
        testUser.setRole("ADMIN");
        testUser.setEnabled(true);
    }

    @Test
    void shouldLoginSuccessfully_WithCorrectCredentials() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testadmin");
        loginRequest.setPassword("Test@123");

        when(authService.authenticate("testadmin", "Test@123")).thenReturn(testUser);
        doNothing().when(authService).login(any(), any());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.username").value("testadmin"))
                .andExpect(jsonPath("$.fullName").value("Test Admin"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void shouldRejectLogin_WithIncorrectCredentials() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testadmin");
        loginRequest.setPassword("WrongPassword");

        when(authService.authenticate(anyString(), anyString())).thenThrow(new IllegalArgumentException("Invalid username or password."));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid username or password."));
    }

    @Test
    void shouldReturnMe_WhenSessionIsValid() throws Exception {
        when(authService.getCurrentUser(any())).thenReturn(Optional.of(testUser));

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.username").value("testadmin"));
    }

    @Test
    void shouldReturnUnauthorizedForMe_WhenSessionIsMissing() throws Exception {
        when(authService.getCurrentUser(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldLogoutSuccessfully() throws Exception {
        doNothing().when(authService).logout(any());

        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldProtectExitApiFromUnauthenticatedUser() throws Exception {
        mockMvc.perform(get("/api/exit/active-bookings"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void shouldAllowExitApiForAuthenticatedUser() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AuthService.SESSION_USER_KEY, testUser);

        // Since ExitController is not in the slice, we expect 404 once it passes interceptor preHandle
        mockMvc.perform(get("/api/exit/active-bookings").session(session))
                .andExpect(status().isNotFound());
    }
}
