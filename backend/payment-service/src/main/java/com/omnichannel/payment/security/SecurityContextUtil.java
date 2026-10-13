
package com.omnichannel.payment.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SecurityContextUtil {

    private final JwtUtil jwtUtil;

    public UUID getCurrentUserId(HttpServletRequest request) {
        String token = extractValidToken(request);
        String subject = jwtUtil.extractSubject(token);

        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("User ID not found in token");
        }

        return UUID.fromString(subject);
    }

    public boolean hasRole(HttpServletRequest request, String role) {
        String token = extractValidToken(request);
        Object rolesClaim = jwtUtil.getClaimFromToken(token, "roles");

        if (!(rolesClaim instanceof Collection<?> roles)) {
            return false;
        }

        String expectedRole = role.startsWith("ROLE_")
                ? role
                : "ROLE_" + role;

        return roles.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .anyMatch(value ->
                        value.equalsIgnoreCase(expectedRole)
                                || value.equalsIgnoreCase(role)
                );
    }

    public void validateAdminRole(HttpServletRequest request) {
        if (!hasRole(request, "ADMIN")) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Admin role required"
            );
        }
    }

    public void validateOwnership(
            HttpServletRequest request,
            UUID resourceOwnerId
    ) {
        UUID currentUserId = getCurrentUserId(request);

        if (!currentUserId.equals(resourceOwnerId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You do not own this resource"
            );
        }
    }

    private String extractValidToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new org.springframework.security.authentication.BadCredentialsException(
                    "Missing Bearer token"
            );
        }

        String token = authHeader.substring(7);

        if (!jwtUtil.isTokenValid(token)) {
            throw new org.springframework.security.authentication.BadCredentialsException(
                    "Invalid or expired JWT"
            );
        }

        return token;
    }
}
