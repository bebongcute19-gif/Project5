
package com.omnichannel.payment.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    private SecretKey getSigningKey() {
        byte[] keyBytes;

        try {
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (Exception e) {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }

        // Giữ đúng cơ chế tạo khóa của Identity Service
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];

            System.arraycopy(
                    keyBytes,
                    0,
                    padded,
                    0,
                    Math.min(keyBytes.length, 32)
            );

            keyBytes = padded;
        }

        return Keys.hmacShaKeyFor(keyBytes);
    }

    public Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    @SuppressWarnings("unchecked")
    public <T> T getClaimFromToken(
            String token,
            String claimKey
    ) {
        try {
            Claims claims = getClaimsFromToken(token);

            if ("sub".equals(claimKey)) {
                return (T) claims.getSubject();
            }

            return (T) claims.get(claimKey);

        } catch (Exception e) {
            log.debug(
                    "Cannot read JWT claim '{}': {}",
                    claimKey,
                    e.getMessage()
            );
            return null;
        }
    }

    public String extractSubject(String token) {
        try {
            return getClaimsFromToken(token).getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isTokenValid(String token) {
        try {
            getClaimsFromToken(token);
            return true;

        } catch (Exception e) {
            log.warn(
                    "Payment JWT validation failed: {}",
                    e.getMessage()
            );
            return false;
        }
    }
}
