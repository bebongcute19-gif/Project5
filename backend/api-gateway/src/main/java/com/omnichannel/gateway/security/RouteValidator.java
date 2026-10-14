package com.omnichannel.gateway.security;

import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.List;

@Component
public class RouteValidator {

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private static final List<String> PUBLIC_GET_ENDPOINTS = List.of(
            "/api/v1/products",
            "/api/v1/products/**",
            "/api/v1/categories",
            "/api/v1/categories/**",
            "/api/v1/brands",
            "/api/v1/brands/**",
            "/api/v1/inventory/*",
            "/health",
            "/api/v1/health",
            "/api/v1/auth/health",
            "/actuator/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    );

    private static final List<String> PUBLIC_POST_ENDPOINTS = List.of(
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/refresh-token"
    );

    private static final List<String> ADMIN_PATHS = List.of(
            "/api/v1/inventory",
            "/api/v1/inventory/admin/**",
            "/api/v1/inventory/adjust"
    );

    private static final List<String> ADMIN_MUTATING_PRODUCT_PATHS = List.of(
            "/api/v1/products",
            "/api/v1/products/**",
            "/api/v1/categories",
            "/api/v1/categories/**",
            "/api/v1/brands",
            "/api/v1/brands/**"
    );

    public boolean isPublic(ServerHttpRequest request) {
        String path = request.getURI().getPath();
        HttpMethod method = request.getMethod();

        if (HttpMethod.GET.equals(method)) {
            for (String pattern : PUBLIC_GET_ENDPOINTS) {
                if (pathMatcher.match(pattern, path)) {
                    // If path also matches an admin-only pattern, do not treat as public
                    for (String adminPattern : ADMIN_PATHS) {
                        if (pathMatcher.match(adminPattern, path)) {
                            return false;
                        }
                    }
                    return true;
                }
            }
        }

        if (HttpMethod.POST.equals(method)) {
            for (String pattern : PUBLIC_POST_ENDPOINTS) {
                if (pathMatcher.match(pattern, path)) {
                    return true;
                }
            }
        }

        if (HttpMethod.OPTIONS.equals(method)) {
            return true;
        }

        return false;
    }

    public boolean isAdminOnly(ServerHttpRequest request) {
        String path = request.getURI().getPath();
        HttpMethod method = request.getMethod();

        // Inventory APIs are Admin only for all methods
        for (String pattern : ADMIN_PATHS) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }

        // POST, PUT, DELETE, PATCH on products, categories, brands are Admin only
        if (HttpMethod.POST.equals(method) || HttpMethod.PUT.equals(method) ||
                HttpMethod.DELETE.equals(method) || HttpMethod.PATCH.equals(method)) {
            for (String pattern : ADMIN_MUTATING_PRODUCT_PATHS) {
                if (pathMatcher.match(pattern, path)) {
                    return true;
                }
            }
        }

        // Users endpoint (excluding /me)
        if (pathMatcher.match("/api/v1/users", path)) {
            return true; // GET /api/v1/users is admin only
        }

        if (pathMatcher.match("/api/v1/users/**", path) &&
                !pathMatcher.match("/api/v1/users/me", path) &&
                !pathMatcher.match("/api/v1/users/me/**", path)) {
            return true;
        }

        return false;
    }
}
