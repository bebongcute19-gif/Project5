package com.omnichannel.identity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnichannel.identity.dto.request.AddressRequest;
import com.omnichannel.identity.dto.request.ChangePasswordRequest;
import com.omnichannel.identity.dto.request.UpdateProfileRequest;
import com.omnichannel.identity.dto.request.UpdateStatusRequest;
import com.omnichannel.identity.dto.response.AddressResponse;
import com.omnichannel.identity.entity.Address;
import com.omnichannel.identity.entity.Role;
import com.omnichannel.identity.entity.User;
import com.omnichannel.identity.repository.AddressRepository;
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

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User adminUser;
    private User customerUser;
    private User otherCustomerUser;
    private String adminToken;
    private String customerToken;
    private String otherCustomerToken;

    @BeforeEach
    void setUp() {
        addressRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();

        Role adminRole = roleRepository.save(Role.builder().name("ADMIN").description("Administrator").build());
        Role customerRole = roleRepository.save(Role.builder().name("CUSTOMER").description("Customer").build());

        adminUser = userRepository.save(User.builder()
                .username("admin")
                .email("admin@example.com")
                .passwordHash(passwordEncoder.encode("AdminPass123!"))
                .fullName("System Admin")
                .status("ACTIVE")
                .enabled(true)
                .roles(Set.of(adminRole))
                .build());

        customerUser = userRepository.save(User.builder()
                .username("customer1")
                .email("customer1@example.com")
                .passwordHash(passwordEncoder.encode("CustomerPass123!"))
                .fullName("Customer One")
                .status("ACTIVE")
                .enabled(true)
                .roles(Set.of(customerRole))
                .build());

        otherCustomerUser = userRepository.save(User.builder()
                .username("customer2")
                .email("customer2@example.com")
                .passwordHash(passwordEncoder.encode("CustomerPass123!"))
                .fullName("Customer Two")
                .status("ACTIVE")
                .enabled(true)
                .roles(Set.of(customerRole))
                .build());

        adminToken = jwtTokenProvider.generateAccessToken(UserPrincipal.create(adminUser));
        customerToken = jwtTokenProvider.generateAccessToken(UserPrincipal.create(customerUser));
        otherCustomerToken = jwtTokenProvider.generateAccessToken(UserPrincipal.create(otherCustomerUser));
    }

    @Test
    @DisplayName("CUSTOMER accessing GET /api/v1/users should return 403 Forbidden")
    void testCustomerCannotAccessAdminUserList() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Forbidden"));
    }

    @Test
    @DisplayName("ADMIN accessing GET /api/v1/users should return 200 OK with list")
    void testAdminCanAccessUserList() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    @DisplayName("CUSTOMER can get and update profile")
    void testCustomerProfileOperations() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("customer1@example.com"))
                .andExpect(jsonPath("$.fullName").value("Customer One"));

        UpdateProfileRequest updateReq = UpdateProfileRequest.builder()
                .fullName("Customer One Updated")
                .phone("0988776655")
                .build();

        mockMvc.perform(put("/api/v1/users/me")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Customer One Updated"))
                .andExpect(jsonPath("$.phone").value("0988776655"));
    }

    @Test
    @DisplayName("CUSTOMER can change password")
    void testChangePassword() throws Exception {
        ChangePasswordRequest changeReq = ChangePasswordRequest.builder()
                .currentPassword("CustomerPass123!")
                .newPassword("NewCustomerPass123!")
                .build();

        mockMvc.perform(put("/api/v1/users/me/password")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changeReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password changed successfully"));
    }

    @Test
    @DisplayName("Address management and cross-user isolation test")
    void testAddressManagementAndIsolation() throws Exception {
        // 1. Create address for customer 1
        AddressRequest addressRequest = AddressRequest.builder()
                .recipientName("Customer One")
                .phone("0901234567")
                .streetAddress("123 Nguyen Hue")
                .ward("Phuong Ben Nghe")
                .district("Quan 1")
                .city("Ho Chi Minh")
                .isDefault(true)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/users/me/addresses")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addressRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recipientName").value("Customer One"))
                .andExpect(jsonPath("$.isDefault").value(true))
                .andReturn();

        AddressResponse addressResponse = objectMapper.readValue(result.getResponse().getContentAsString(), AddressResponse.class);
        UUID addressId = addressResponse.getId();

        // 2. Customer 2 attempts to modify Customer 1's address -> 404 Not Found
        AddressRequest maliciousUpdate = AddressRequest.builder()
                .recipientName("Hacked Name")
                .phone("0999999999")
                .streetAddress("Hacked St")
                .ward("Ward")
                .district("District")
                .city("City")
                .build();

        mockMvc.perform(put("/api/v1/users/me/addresses/" + addressId)
                        .header("Authorization", "Bearer " + otherCustomerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(maliciousUpdate)))
                .andExpect(status().isNotFound());

        // 3. Customer 2 attempts to delete Customer 1's address -> 404 Not Found
        mockMvc.perform(delete("/api/v1/users/me/addresses/" + addressId)
                        .header("Authorization", "Bearer " + otherCustomerToken))
                .andExpect(status().isNotFound());

        // 4. Customer 1 can update their own address
        AddressRequest validUpdate = AddressRequest.builder()
                .recipientName("Customer One Modified")
                .phone("0901234567")
                .streetAddress("123 Nguyen Hue")
                .ward("Phuong Ben Nghe")
                .district("Quan 1")
                .city("Ho Chi Minh")
                .isDefault(true)
                .build();

        mockMvc.perform(put("/api/v1/users/me/addresses/" + addressId)
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recipientName").value("Customer One Modified"));

        // 5. Customer 1 can delete their own address
        mockMvc.perform(delete("/api/v1/users/me/addresses/" + addressId)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Admin cannot deactivate the last remaining admin")
    void testAdminCannotDeactivateLastAdmin() throws Exception {
        UpdateStatusRequest deactivateRequest = UpdateStatusRequest.builder()
                .status("LOCKED")
                .enabled(false)
                .build();

        mockMvc.perform(patch("/api/v1/users/" + adminUser.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deactivateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Cannot deactivate or lock the last remaining administrator account"));
    }
}
