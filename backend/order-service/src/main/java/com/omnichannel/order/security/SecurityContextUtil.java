package com.omnichannel.order.security;

import com.omnichannel.order.exception.ForbiddenException;
import com.omnichannel.order.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class SecurityContextUtil {

    public UUID getCurrentUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        if (userId == null) {
            throw new UnauthorizedException("User ID not found in request context");
        }
        return (UUID) userId;
    }

    public String getCurrentUsername(HttpServletRequest request) {
        Object username = request.getAttribute("username");
        if (username == null) {
            throw new UnauthorizedException("Username not found in request context");
        }
        return (String) username;
    }

    public List<String> getCurrentUserRoles(HttpServletRequest request) {
        Object roles = request.getAttribute("roles");
        if (roles == null) {
            throw new UnauthorizedException("Roles not found in request context");
        }
        return (List<String>) roles;
    }

    public boolean hasRole(HttpServletRequest request, String role) {
        List<String> roles = getCurrentUserRoles(request);
        return roles != null && roles.contains("ROLE_" + role);
    }

    public void validateOwnership(HttpServletRequest request, UUID resourceOwnerId) {
        UUID currentUserId = getCurrentUserId(request);
        if (!currentUserId.equals(resourceOwnerId)) {
            throw new ForbiddenException("You don't have permission to access this resource");
        }
    }

    public void validateAdminRole(HttpServletRequest request) {
        if (!hasRole(request, "ADMIN")) {
            throw new ForbiddenException("Admin role is required for this operation");
        }
    }
}
