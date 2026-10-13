package com.omnichannel.identity.service;

import com.omnichannel.identity.config.JwtProperties;
import com.omnichannel.identity.dto.request.LoginRequest;
import com.omnichannel.identity.dto.request.RefreshTokenRequest;
import com.omnichannel.identity.dto.request.RegisterRequest;
import com.omnichannel.identity.dto.response.MessageResponse;
import com.omnichannel.identity.dto.response.TokenResponse;
import com.omnichannel.identity.dto.response.UserResponse;
import com.omnichannel.identity.entity.RefreshToken;
import com.omnichannel.identity.entity.Role;
import com.omnichannel.identity.entity.User;
import com.omnichannel.identity.exception.DuplicateResourceException;
import com.omnichannel.identity.exception.UnauthorizedException;
import com.omnichannel.identity.repository.RefreshTokenRepository;
import com.omnichannel.identity.repository.RoleRepository;
import com.omnichannel.identity.repository.UserRepository;
import com.omnichannel.identity.security.JwtTokenProvider;
import com.omnichannel.identity.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private User sampleUser;
    private Role customerRole;

    @BeforeEach
    void setUp() {
        customerRole = Role.builder().id(1L).name("CUSTOMER").description("Customer").build();
        Set<Role> roles = new HashSet<>();
        roles.add(customerRole);

        sampleUser = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .email("test@example.com")
                .passwordHash("encodedPassword")
                .fullName("Test User")
                .phone("0123456789")
                .status("ACTIVE")
                .enabled(true)
                .roles(roles)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Register with valid request should return UserResponse")
    void testRegister_Success() {
        RegisterRequest request = RegisterRequest.builder()
                .username("newuser")
                .email("new@example.com")
                .password("Password123!")
                .fullName("New User")
                .phone("0987654321")
                .build();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userRepository.existsByUsername(request.getUsername())).thenReturn(false);
        when(roleRepository.findByName("CUSTOMER")).thenReturn(Optional.of(customerRole));
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        UserResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals(sampleUser.getEmail(), response.getEmail());
        assertTrue(response.getRoles().contains("ROLE_CUSTOMER"));
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Register with duplicate email should throw DuplicateResourceException")
    void testRegister_DuplicateEmail_ThrowsException() {
        RegisterRequest request = RegisterRequest.builder()
                .username("newuser")
                .email("existing@example.com")
                .password("Password123!")
                .fullName("New User")
                .build();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Register with duplicate username should throw DuplicateResourceException")
    void testRegister_DuplicateUsername_ThrowsException() {
        RegisterRequest request = RegisterRequest.builder()
                .username("existinguser")
                .email("new@example.com")
                .password("Password123!")
                .fullName("New User")
                .build();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userRepository.existsByUsername(request.getUsername())).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Login with valid credentials should return TokenResponse")
    void testLogin_Success() {
        LoginRequest request = LoginRequest.builder()
                .email("test@example.com")
                .password("Password123!")
                .build();

        UserPrincipal principal = UserPrincipal.create(sampleUser);
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authToken);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(jwtTokenProvider.generateAccessToken(principal)).thenReturn("jwt.access.token");
        when(jwtTokenProvider.getRefreshTokenExpirationMs()).thenReturn(604800000L);
        when(jwtTokenProvider.getAccessTokenExpirationMs()).thenReturn(900000L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArguments()[0]);

        TokenResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("jwt.access.token", response.getAccessToken());
        assertEquals("Bearer", response.getTokenType());
        assertNotNull(response.getRefreshToken());
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Login with wrong password should throw UnauthorizedException")
    void testLogin_WrongPassword_ThrowsException() {
        LoginRequest request = LoginRequest.builder()
                .email("test@example.com")
                .password("WrongPassword")
                .build();

        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(UnauthorizedException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("RefreshToken should rotate token and return new TokenResponse")
    void testRefreshToken_Success_Rotation() {
        String oldToken = "old-refresh-token";
        RefreshToken existingRefreshToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(sampleUser.getId())
                .token(oldToken)
                .expiresAt(Instant.now().plus(1, ChronoUnit.DAYS))
                .isRevoked(false)
                .build();

        RefreshTokenRequest request = RefreshTokenRequest.builder().refreshToken(oldToken).build();

        when(refreshTokenRepository.findByToken(oldToken)).thenReturn(Optional.of(existingRefreshToken));
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(jwtTokenProvider.generateAccessToken(any())).thenReturn("new.access.token");
        when(jwtTokenProvider.getRefreshTokenExpirationMs()).thenReturn(604800000L);
        when(jwtTokenProvider.getAccessTokenExpirationMs()).thenReturn(900000L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArguments()[0]);

        TokenResponse response = authService.refreshToken(request);

        assertNotNull(response);
        assertEquals("new.access.token", response.getAccessToken());
        assertNotEquals(oldToken, response.getRefreshToken());
        assertTrue(existingRefreshToken.getIsRevoked());
        assertNotNull(existingRefreshToken.getRevokedAt());
    }

    @Test
    @DisplayName("RefreshToken with expired token should throw UnauthorizedException")
    void testRefreshToken_Expired_ThrowsException() {
        String token = "expired-token";
        RefreshToken expiredToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(sampleUser.getId())
                .token(token)
                .expiresAt(Instant.now().minus(1, ChronoUnit.HOURS))
                .isRevoked(false)
                .build();

        RefreshTokenRequest request = RefreshTokenRequest.builder().refreshToken(token).build();
        when(refreshTokenRepository.findByToken(token)).thenReturn(Optional.of(expiredToken));

        assertThrows(UnauthorizedException.class, () -> authService.refreshToken(request));
        verify(refreshTokenRepository, times(1)).revokeAllByUserId(eq(sampleUser.getId()), any());
    }

    @Test
    @DisplayName("Logout should revoke tokens and return MessageResponse")
    void testLogout_Success() {
        UUID userId = sampleUser.getId();
        String token = "some-token";
        RefreshToken refreshToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .token(token)
                .isRevoked(false)
                .build();

        when(refreshTokenRepository.findByToken(token)).thenReturn(Optional.of(refreshToken));

        MessageResponse response = authService.logout(userId, token);

        assertNotNull(response);
        assertEquals("Logged out successfully", response.getMessage());
        assertTrue(refreshToken.getIsRevoked());
        verify(refreshTokenRepository, times(1)).revokeAllByUserId(eq(userId), any());
    }
}
