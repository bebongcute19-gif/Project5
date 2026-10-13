package com.omnichannel.identity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnichannel.identity.dto.request.LoginRequest;
import com.omnichannel.identity.dto.request.RefreshTokenRequest;
import com.omnichannel.identity.dto.request.RegisterRequest;
import com.omnichannel.identity.dto.response.TokenResponse;
import com.omnichannel.identity.dto.response.UserResponse;
import com.omnichannel.identity.entity.Role;
import com.omnichannel.identity.entity.User;
import com.omnichannel.identity.repository.RefreshTokenRepository;
import com.omnichannel.identity.repository.RoleRepository;
import com.omnichannel.identity.repository.UserRepository;
import com.omnichannel.identity.security.JwtTokenProvider;
import com.omnichannel.identity.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();

        roleRepository.save(Role.builder().name("CUSTOMER").description("Customer").build());
        roleRepository.save(Role.builder().name("ADMIN").description("Administrator").build());
    }

    @Test
    @DisplayName("POST /api/v1/auth/register should create user and return 201")
    void testRegister_Success() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("john_doe")
                .email("john@example.com")
                .password("Password123!")
                .fullName("John Doe")
                .phone("0901234567")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.username").value("john_doe"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_CUSTOMER"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/v1/auth/register duplicate email should return 409 Conflict")
    void testRegister_DuplicateEmail() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("john_doe")
                .email("john@example.com")
                .password("Password123!")
                .fullName("John Doe")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflict"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login with valid and invalid credentials")
    void testLogin_Flow() throws Exception {
        Role customerRole = roleRepository.findByName("CUSTOMER").orElseThrow();
        User user = User.builder()
                .username("alice")
                .email("alice@example.com")
                .passwordHash(passwordEncoder.encode("Secret123!"))
                .fullName("Alice Wonder")
                .status("ACTIVE")
                .enabled(true)
                .roles(Set.of(customerRole))
                .build();
        userRepository.save(user);

        // 1. Success login
        LoginRequest validLogin = LoginRequest.builder()
                .email("alice@example.com")
                .password("Secret123!")
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn();

        TokenResponse tokenResponse = objectMapper.readValue(result.getResponse().getContentAsString(), TokenResponse.class);
        assertNotNull(tokenResponse.getAccessToken());

        // 2. Failed login
        LoginRequest invalidLogin = LoginRequest.builder()
                .email("alice@example.com")
                .password("WrongPass!")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidLogin)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Unauthorized"));

        // 3. Refresh token rotation
        RefreshTokenRequest refreshReq = RefreshTokenRequest.builder()
                .refreshToken(tokenResponse.getRefreshToken())
                .build();

        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();

        TokenResponse rotatedResponse = objectMapper.readValue(refreshResult.getResponse().getContentAsString(), TokenResponse.class);
        assertNotEquals(tokenResponse.getRefreshToken(), rotatedResponse.getRefreshToken());

        // 4. Access /me endpoint with Bearer token
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + rotatedResponse.getAccessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.fullName").value("Alice Wonder"));

        // 5. Logout
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + rotatedResponse.getAccessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(RefreshTokenRequest.builder().refreshToken(rotatedResponse.getRefreshToken()).build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));
    }
}
