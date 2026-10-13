package com.omnichannel.gateway.security;

import com.omnichannel.gateway.config.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtValidatorTest {

    private JwtValidator jwtValidator;
    private JwtProperties jwtProperties;
    private final String secret = "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY3ODkwYWJjZGVmZ2hpamtsbW5vcA==";
    private SecretKey key;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setSecret(secret);
        jwtValidator = new JwtValidator(jwtProperties);

        byte[] keyBytes = Decoders.BASE64.decode(secret);
        key = Keys.hmacShaKeyFor(keyBytes);
    }

    @Test
    @DisplayName("Valid JWT token should pass validation and extract claims")
    void testValidateToken_Success() {
        UUID userId = UUID.randomUUID();
        String token = Jwts.builder()
                .subject(userId.toString())
                .claim("username", "testuser")
                .claim("email", "test@example.com")
                .claim("roles", List.of("ROLE_CUSTOMER"))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        assertTrue(jwtValidator.validateToken(token));
        assertEquals(userId.toString(), jwtValidator.getUserId(token));
        assertEquals("testuser", jwtValidator.getUsername(token));
        assertEquals("test@example.com", jwtValidator.getEmail(token));
        assertEquals(List.of("ROLE_CUSTOMER"), jwtValidator.getRoles(token));
    }

    @Test
    @DisplayName("Expired JWT token should fail validation")
    void testValidateToken_Expired() {
        String token = Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .issuedAt(new Date(System.currentTimeMillis() - 120000))
                .expiration(new Date(System.currentTimeMillis() - 60000))
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        assertFalse(jwtValidator.validateToken(token));
    }

    @Test
    @DisplayName("Invalid signature should fail validation")
    void testValidateToken_InvalidSignature() {
        SecretKey otherKey = Keys.hmacShaKeyFor("differentSecretKeyForSigningPurposesOnly123456".getBytes());
        String token = Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(otherKey, Jwts.SIG.HS256)
                .compact();

        assertFalse(jwtValidator.validateToken(token));
    }

    @Test
    @DisplayName("Empty or null token should fail validation")
    void testValidateToken_NullOrEmpty() {
        assertFalse(jwtValidator.validateToken(null));
        assertFalse(jwtValidator.validateToken(""));
        assertFalse(jwtValidator.validateToken("invalid.jwt.string"));
    }
}
