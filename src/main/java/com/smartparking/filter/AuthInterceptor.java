package com.smartparking.filter;

import com.smartparking.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor to secure /api/exit/** endpoints and prevent caching of sensitive pages.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();

        // 1. Disable caching for exit.html to prevent back-button access after logout
        if (uri.endsWith("/exit.html")) {
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setDateHeader("Expires", 0);
        }

        // 2. Protect /api/exit/** endpoints
        if (uri.startsWith("/api/exit")) {
            HttpSession session = request.getSession(false);
            if (session == null || session.getAttribute(AuthService.SESSION_USER_KEY) == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write("{\"success\":false,\"message\":\"Unauthorized access. Please login.\"}\n");
                return false;
            }
        }

        return true;
    }
}
