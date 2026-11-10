package com.smartparking.filter;

import com.smartparking.entity.StaffUser;
import com.smartparking.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthInterceptorTest {

    @InjectMocks
    private AuthInterceptor authInterceptor;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Test
    void analyticsEndpoints_requireSession() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/analytics/booking-history");
        when(request.getSession(false)).thenReturn(null);
        when(response.getWriter()).thenReturn(new java.io.PrintWriter(new java.io.StringWriter()));

        boolean allowed = authInterceptor.preHandle(request, response, new Object());

        assertThat(allowed).isFalse();
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    void analyticsEndpoints_allowAuthenticatedSession() throws Exception {
        HttpSession session = org.mockito.Mockito.mock(HttpSession.class);
        when(request.getRequestURI()).thenReturn("/api/analytics/bookings/42/history");
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(AuthService.SESSION_USER_KEY)).thenReturn(new StaffUser());

        boolean allowed = authInterceptor.preHandle(request, response, new Object());

        assertThat(allowed).isTrue();
    }

    @Test
    void analyticsEndpoints_setNoStoreBeforeAuth() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/analytics/overview");
        when(request.getSession(false)).thenReturn(null);
        when(response.getWriter()).thenReturn(new java.io.PrintWriter(new java.io.StringWriter()));

        authInterceptor.preHandle(request, response, new Object());

        // Cache headers are applied even to unauthenticated responses
        verify(response).setHeader("Cache-Control", "no-store");
        verify(response).setHeader("Pragma", "no-cache");
    }

    @Test
    void nonProtectedEndpoints_passThrough() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/parking-slots");

        boolean allowed = authInterceptor.preHandle(request, response, new Object());

        assertThat(allowed).isTrue();
    }
}
