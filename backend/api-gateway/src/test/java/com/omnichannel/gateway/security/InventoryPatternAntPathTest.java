package com.omnichannel.gateway.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.util.AntPathMatcher;

import static org.junit.jupiter.api.Assertions.*;

class InventoryPatternAntPathTest {

    @Test
    void testInventorySkuPatternMatches() {
        AntPathMatcher matcher = new AntPathMatcher();

        String pattern = "/api/v1/inventory/*";

        // SKU path should match
        assertTrue(matcher.match(pattern, "/api/v1/inventory/SKU-IP15P-BLK-256"), "Pattern should match single-segment SKU path");

        // Admin path with two segments after inventory should NOT match single-segment pattern
        assertFalse(matcher.match(pattern, "/api/v1/inventory/admin/foo"), "Admin/foo should not match single-segment pattern");

        // The literal 'adjust' is a single segment and WILL match the pattern at AntPathMatcher level
        assertTrue(matcher.match(pattern, "/api/v1/inventory/adjust"), "AntPathMatcher: /api/v1/inventory/* matches '/api/v1/inventory/adjust'");

        // Root inventory without trailing segment should not match
        assertFalse(matcher.match(pattern, "/api/v1/inventory"), "Root inventory path (no segment) should not match single-segment pattern");

        // Double-star would match deeper paths - verify behaviour with /** for reference
        String deepPattern = "/api/v1/inventory/**";
        assertTrue(matcher.match(deepPattern, "/api/v1/inventory/admin/foo"), "Deep pattern should match admin path");
    }

    @Test
    void testRouteValidatorExcludesAdminFromPublic() {
        RouteValidator rv = new RouteValidator();

        // Public SKU should be considered public
        assertTrue(rv.isPublic(MockServerHttpRequest.get("/api/v1/inventory/SKU-IP15P-BLK-256").build()));

        // Admin and adjust must NOT be public
        assertFalse(rv.isPublic(MockServerHttpRequest.get("/api/v1/inventory/admin/foo").build()));
        assertFalse(rv.isPublic(MockServerHttpRequest.get("/api/v1/inventory/adjust").build()));

        // And they should be flagged admin-only
        assertTrue(rv.isAdminOnly(MockServerHttpRequest.get("/api/v1/inventory/admin/foo").build()));
        assertTrue(rv.isAdminOnly(MockServerHttpRequest.post("/api/v1/inventory/adjust").build()));
    }
}
