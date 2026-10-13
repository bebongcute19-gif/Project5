package com.omnichannel.identity.service;

import com.omnichannel.identity.dto.request.LoginRequest;
import com.omnichannel.identity.dto.request.RefreshTokenRequest;
import com.omnichannel.identity.dto.request.RegisterRequest;
import com.omnichannel.identity.dto.response.MessageResponse;
import com.omnichannel.identity.dto.response.TokenResponse;
import com.omnichannel.identity.dto.response.UserResponse;
import com.omnichannel.identity.entity.RefreshToken;
import com.omnichannel.identity.entity.Role;
import com.omnichannel.identity.entity.User;
import com.omnichannel.identity.exception.BadRequestException;
import com.omnichannel.identity.exception.DuplicateResourceException;
import com.omnichannel.identity.exception.ResourceNotFoundException;
import com.omnichannel.identity.exception.UnauthorizedException;
import com.omnichannel.identity.repository.RefreshTokenRepository;
import com.omnichannel.identity.repository.RoleRepository;
import com.omnichannel.identity.repository.UserRepository;
import com.omnichannel.identity.security.JwtTokenProvider;
import com.omnichannel.identity.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email is already registered: " + request.getEmail());
        }

        if (request.getUsername() != null && userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username is already in use: " + request.getUsername());
        }

        Role customerRole = roleRepository.findByName("CUSTOMER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("CUSTOMER").description("Customer").build()));

        Set<Role> roles = new HashSet<>();
        roles.add(customerRole);

        User user = User.builder()
                .username(request.getUsername() != null && !request.getUsername().isBlank() ? request.getUsername() : request.getEmail())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .status("ACTIVE")
                .enabled(true)
                .roles(roles)
                .build();

        User savedUser = userRepository.save(user);
        return mapToUserResponse(savedUser);
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            User user = userRepository.findById(userPrincipal.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            if (!Boolean.TRUE.equals(user.getEnabled()) || "LOCKED".equalsIgnoreCase(user.getStatus()) || "INACTIVE".equalsIgnoreCase(user.getStatus())) {
                throw new UnauthorizedException("User account is inactive or locked");
            }

            String accessToken = jwtTokenProvider.generateAccessToken(userPrincipal);
            String rawRefreshToken = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
            String tokenHash = hashToken(rawRefreshToken);

            RefreshToken refreshToken = RefreshToken.builder()
                    .userId(user.getId())
                    .token(rawRefreshToken)
                    .tokenHash(tokenHash)
                    .expiresAt(Instant.now().plusMillis(jwtTokenProvider.getRefreshTokenExpirationMs()))
                    .isRevoked(false)
                    .build();

            refreshTokenRepository.save(refreshToken);

            return TokenResponse.builder()
                    .accessToken(accessToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtTokenProvider.getAccessTokenExpirationMs() / 1000)
                    .refreshToken(rawRefreshToken)
                    .build();

        } catch (BadCredentialsException e) {
            throw new UnauthorizedException("Invalid email/username or password");
        }
    }

    @Transactional
    public TokenResponse refreshToken(RefreshTokenRequest request) {
        String rawToken = request.getRefreshToken();
        if (rawToken == null || rawToken.isBlank()) {
            throw new BadRequestException("Refresh token is required");
        }

        RefreshToken refreshToken = refreshTokenRepository.findByToken(rawToken)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (Boolean.TRUE.equals(refreshToken.getIsRevoked()) || refreshToken.getExpiresAt().isBefore(Instant.now())) {
            // Revoke all tokens for this user if token reuse/tampering is detected
            refreshTokenRepository.revokeAllByUserId(refreshToken.getUserId(), Instant.now());
            throw new UnauthorizedException("Refresh token is revoked or expired");
        }

        // Token rotation: Revoke current token
        refreshToken.setIsRevoked(true);
        refreshToken.setRevokedAt(Instant.now());
        refreshTokenRepository.save(refreshToken);

        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!Boolean.TRUE.equals(user.getEnabled()) || "LOCKED".equalsIgnoreCase(user.getStatus())) {
            throw new UnauthorizedException("User account is disabled or locked");
        }

        UserPrincipal userPrincipal = UserPrincipal.create(user);
        String newAccessToken = jwtTokenProvider.generateAccessToken(userPrincipal);
        String newRawRefreshToken = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        String newTokenHash = hashToken(newRawRefreshToken);

        RefreshToken newRefreshToken = RefreshToken.builder()
                .userId(user.getId())
                .token(newRawRefreshToken)
                .tokenHash(newTokenHash)
                .expiresAt(Instant.now().plusMillis(jwtTokenProvider.getRefreshTokenExpirationMs()))
                .isRevoked(false)
                .build();

        refreshTokenRepository.save(newRefreshToken);

        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenExpirationMs() / 1000)
                .refreshToken(newRawRefreshToken)
                .build();
    }

    @Transactional
    public MessageResponse logout(UUID userId, String rawRefreshToken) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            refreshTokenRepository.findByToken(rawRefreshToken).ifPresent(rt -> {
                rt.setIsRevoked(true);
                rt.setRevokedAt(Instant.now());
                refreshTokenRepository.save(rt);
            });
        }
        if (userId != null) {
            refreshTokenRepository.revokeAllByUserId(userId, Instant.now());
        }
        return MessageResponse.builder().message("Logged out successfully").build();
    }

    public static UserResponse mapToUserResponse(User user) {
        Set<String> roleNames = user.getRoles() != null
                ? user.getRoles().stream()
                .map(r -> r.getName().startsWith("ROLE_") ? r.getName() : "ROLE_" + r.getName())
                .collect(Collectors.toSet())
                : Set.of();

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .status(user.getStatus())
                .enabled(user.getEnabled())
                .roles(roleNames)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            return token;
        }
    }
}
