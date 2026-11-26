package com.lims.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

/**
 * Records every write request (POST/PUT/DELETE) in audit_logs. Auditing
 * must never break the request itself, so all failures are swallowed.
 */
@Component
public class AuditInterceptor implements HandlerInterceptor {
    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "DELETE");

    private final JdbcTemplate jdbc;

    public AuditInterceptor(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        if (!WRITE_METHODS.contains(request.getMethod())) {
            return;
        }
        try {
            String username = "anonymous";
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                username = authentication.getName();
            }
            jdbc.update("INSERT INTO audit_logs(username, action, request_path, status_code) VALUES (?, ?, ?, ?)",
                username, request.getMethod(), request.getRequestURI(), response.getStatus());
        } catch (Exception ignored) {
            // Audit logging is best-effort only.
        }
    }
}
