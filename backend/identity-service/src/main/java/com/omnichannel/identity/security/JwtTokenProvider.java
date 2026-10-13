
package com.omnichannel.identity.security;

import com.omnichannel.identity.config.JwtProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private static final Logger log =
            LoggerFactory.getLogger(JwtTokenProvider.class);

    private final JwtProperties jwtProperties;

    // TẠM THỜI: Kiểm tra fingerprint của khóa JWT
    public String getSigningKeyFingerprint() {
        try {
            byte[] key = getSigningKey().getEncoded();

            byte[] hash = MessageDigest
                    .getInstance("SHA-256")
                    .digest(key);

            return HexFormat.of()
                    .formatHex(hash)
                    .substring(0, 16);

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    // TẠM THỜI: In fingerprint khi service khởi động
    @PostConstruct
    public void logJwtFingerprint() {
        log.info(
                "JWT signing key fingerprint: {}",
                getSigningKeyFingerprint()
        );
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes;

        try {
            keyBytes = Decoders.BASE64.decode(
                    jwtProperties.getSecret()
            );
        } catch (Exception e) {
            keyBytes = jwtProperties.getSecret()
                    .getBytes(StandardCharsets.UTF_8);
        }

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

    public String generateAccessToken(UserPrincipal userPrincipal) {
        Date now = new Date();

        Date expiryDate = new Date(
                now.getTime()
                        + jwtProperties.getAccessTokenExpirationMs()
        );

        List<String> roles = userPrincipal.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .filter(auth -> auth.startsWith("ROLE_"))
                .toList();

        return Jwts.builder()
                .subject(userPrincipal.getId().toString())
                .claim("username", userPrincipal.getUsername())
                .claim("email", userPrincipal.getEmail())
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    public Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UUID getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return UUID.fromString(claims.getSubject());
    }

    public String getUsernameFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("username", String.class);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);

            return true;

        } catch (SecurityException | MalformedJwtException e) {
            log.warn(
                    "Invalid JWT signature: {}",
                    e.getMessage()
            );

        } catch (ExpiredJwtException e) {
            log.warn(
                    "Expired JWT token: {}",
                    e.getMessage()
            );

        } catch (UnsupportedJwtException e) {
            log.warn(
                    "Unsupported JWT token: {}",
                    e.getMessage()
            );

        } catch (IllegalArgumentException e) {
            log.warn(
                    "JWT claims string is empty: {}",
                    e.getMessage()
            );
        }

        return false;
    }

    public long getAccessTokenExpirationMs() {
        return jwtProperties.getAccessTokenExpirationMs();
    }

    public long getRefreshTokenExpirationMs() {
        return jwtProperties.getRefreshTokenExpirationMs();
    }
}
