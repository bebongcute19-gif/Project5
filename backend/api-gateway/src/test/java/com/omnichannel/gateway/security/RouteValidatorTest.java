package com.omnichannel.gateway.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouteValidatorTest {

    private RouteValidator routeValidator;

    @BeforeEach
    void setUp() {
        routeValidator = new RouteValidator();
    }

    @Test
    @DisplayName("Public routes should return isPublic == true")
    void testPublicRoutes() {
        assertTrue(routeValidator.isPublic(MockServerHttpRequest.post("/api/v1/auth/register").build()));
        assertTrue(routeValidator.isPublic(MockServerHttpRequest.post("/api/v1/auth/login").build()));
        assertTrue(routeValidator.isPublic(MockServerHttpRequest.post("/api/v1/auth/refresh").build()));
        assertTrue(routeValidator.isPublic(MockServerHttpRequest.get("/api/v1/products").build()));
        assertTrue(routeValidator.isPublic(MockServerHttpRequest.get("/api/v1/products/123").build()));
        assertTrue(routeValidator.isPublic(MockServerHttpRequest.get("/api/v1/categories").build()));
        assertTrue(routeValidator.isPublic(MockServerHttpRequest.get("/api/v1/brands").build()));
        assertTrue(routeValidator.isPublic(MockServerHttpRequest.get("/health").build()));
        // Inventory SKU read should be public
        assertTrue(routeValidator.isPublic(MockServerHttpRequest.get("/api/v1/inventory/SKU-ABC-123").build()));
    }

    @Test
    @DisplayName("Protected routes should return isPublic == false")
    void testProtectedRoutes() {
        assertFalse(routeValidator.isPublic(MockServerHttpRequest.get("/api/v1/users/me").build()));
        assertFalse(routeValidator.isPublic(MockServerHttpRequest.get("/api/v1/users").build()));
        assertFalse(routeValidator.isPublic(MockServerHttpRequest.post("/api/v1/orders").build()));
        assertFalse(routeValidator.isPublic(MockServerHttpRequest.post("/api/v1/products").build()));
        assertFalse(routeValidator.isPublic(MockServerHttpRequest.delete("/api/v1/categories/1").build()));
    }

    @Test
    @DisplayName("Admin only routes should return isAdminOnly == true")
    void testAdminOnlyRoutes() {
        assertTrue(routeValidator.isAdminOnly(MockServerHttpRequest.get("/api/v1/users").build()));
        assertTrue(routeValidator.isAdminOnly(MockServerHttpRequest.get("/api/v1/users/some-uuid").build()));
        assertTrue(routeValidator.isAdminOnly(MockServerHttpRequest.patch("/api/v1/users/some-uuid/status").build()));
        assertTrue(routeValidator.isAdminOnly(MockServerHttpRequest.post("/api/v1/products").build()));
        assertTrue(routeValidator.isAdminOnly(MockServerHttpRequest.put("/api/v1/categories/1").build()));
        assertTrue(routeValidator.isAdminOnly(MockServerHttpRequest.delete("/api/v1/brands/1").build()));
        assertTrue(routeValidator.isAdminOnly(MockServerHttpRequest.get("/api/v1/inventory").build()));
        assertTrue(routeValidator.isAdminOnly(MockServerHttpRequest.post("/api/v1/inventory/adjust").build()));
    }

    @Test
    @DisplayName("Customer and Public routes should return isAdminOnly == false")
    void testNonAdminRoutes() {
        assertFalse(routeValidator.isAdminOnly(MockServerHttpRequest.get("/api/v1/users/me").build()));
        assertFalse(routeValidator.isAdminOnly(MockServerHttpRequest.put("/api/v1/users/me").build()));
        assertFalse(routeValidator.isAdminOnly(MockServerHttpRequest.get("/api/v1/users/me/addresses").build()));
        assertFalse(routeValidator.isAdminOnly(MockServerHttpRequest.get("/api/v1/products").build()));
        assertFalse(routeValidator.isAdminOnly(MockServerHttpRequest.post("/api/v1/orders").build()));
        // Inventory SKU read is not admin-only
        assertFalse(routeValidator.isAdminOnly(MockServerHttpRequest.get("/api/v1/inventory/SKU-ABC-123").build()));
    }
}
