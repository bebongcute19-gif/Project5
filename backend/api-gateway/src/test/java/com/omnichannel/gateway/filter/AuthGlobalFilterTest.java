package com.omnichannel.gateway.filter;

import com.omnichannel.gateway.config.JwtProperties;
import com.omnichannel.gateway.security.JwtValidator;
import com.omnichannel.gateway.security.RouteValidator;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class AuthGlobalFilterTest {

    private AuthGlobalFilter authGlobalFilter;
    private final String secret = "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY3ODkwYWJjZGVmZ2hpamtsbW5vcA==";
    private SecretKey key;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret(secret);
        JwtValidator jwtValidator = new JwtValidator(jwtProperties);
        RouteValidator routeValidator = new RouteValidator();
        authGlobalFilter = new AuthGlobalFilter(jwtValidator, routeValidator);

        byte[] keyBytes = Decoders.BASE64.decode(secret);
        key = Keys.hmacShaKeyFor(keyBytes);
    }

    private String generateToken(String username, String email, List<String> roles) {
        return Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .claim("username", username)
                .claim("email", email)
                .claim("roles", roles)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    @Test
    @DisplayName("Public route without token should pass through and strip spoofed headers")
    void testPublicRouteWithoutToken() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/products")
                .header("X-User-Id", "spoofed-user-id")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicBoolean filterCalled = new AtomicBoolean(false);
        GatewayFilterChain chain = ex -> {
            filterCalled.set(true);
            assertNull(ex.getRequest().getHeaders().getFirst("X-User-Id"));
            return Mono.empty();
        };

        authGlobalFilter.filter(exchange, chain).block();

        assertTrue(filterCalled.get());
        assertNull(exchange.getResponse().getStatusCode());
    }

    @Test
    @DisplayName("Protected route without token should return 401 Unauthorized")
    void testProtectedRouteWithoutToken() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/users/me").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = ex -> Mono.empty();

        authGlobalFilter.filter(exchange, chain).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    @DisplayName("Protected route with invalid token should return 401 Unauthorized")
    void testProtectedRouteWithInvalidToken() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.token.value")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = ex -> Mono.empty();

        authGlobalFilter.filter(exchange, chain).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    @DisplayName("Customer accessing Admin route should return 403 Forbidden")
    void testCustomerAccessingAdminRoute() {
        String token = generateToken("customer1", "cust@example.com", List.of("ROLE_CUSTOMER"));
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = ex -> Mono.empty();

        authGlobalFilter.filter(exchange, chain).block();

        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
    }

    @Test
    @DisplayName("Admin accessing Admin route should succeed and propagate X-User headers")
    void testAdminAccessingAdminRoute() {
        String token = generateToken("adminUser", "admin@example.com", List.of("ROLE_ADMIN"));
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicBoolean filterCalled = new AtomicBoolean(false);
        GatewayFilterChain chain = ex -> {
            filterCalled.set(true);
            assertEquals("adminUser", ex.getRequest().getHeaders().getFirst("X-User-Username"));
            assertEquals("admin@example.com", ex.getRequest().getHeaders().getFirst("X-User-Email"));
            assertEquals("ROLE_ADMIN", ex.getRequest().getHeaders().getFirst("X-User-Roles"));
            assertNotNull(ex.getRequest().getHeaders().getFirst("X-User-Id"));
            return Mono.empty();
        };

        authGlobalFilter.filter(exchange, chain).block();

        assertTrue(filterCalled.get());
        assertNull(exchange.getResponse().getStatusCode());
    }

    @Test
    @DisplayName("Customer accessing Customer route should succeed and propagate X-User headers")
    void testCustomerAccessingCustomerRoute() {
        String token = generateToken("customerUser", "customer@example.com", List.of("ROLE_CUSTOMER"));
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicBoolean filterCalled = new AtomicBoolean(false);
        GatewayFilterChain chain = ex -> {
            filterCalled.set(true);
            assertEquals("customerUser", ex.getRequest().getHeaders().getFirst("X-User-Username"));
            assertEquals("customer@example.com", ex.getRequest().getHeaders().getFirst("X-User-Email"));
            assertEquals("ROLE_CUSTOMER", ex.getRequest().getHeaders().getFirst("X-User-Roles"));
            return Mono.empty();
        };

        authGlobalFilter.filter(exchange, chain).block();

        assertTrue(filterCalled.get());
        assertNull(exchange.getResponse().getStatusCode());
    }
}
