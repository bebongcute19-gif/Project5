package com.omnichannel.identity.service;

import com.omnichannel.identity.dto.request.ChangePasswordRequest;
import com.omnichannel.identity.dto.request.UpdateProfileRequest;
import com.omnichannel.identity.dto.request.UpdateRolesRequest;
import com.omnichannel.identity.dto.request.UpdateStatusRequest;
import com.omnichannel.identity.dto.response.MessageResponse;
import com.omnichannel.identity.dto.response.PageResponse;
import com.omnichannel.identity.dto.response.UserResponse;
import com.omnichannel.identity.entity.Role;
import com.omnichannel.identity.entity.User;
import com.omnichannel.identity.exception.BadRequestException;
import com.omnichannel.identity.repository.RefreshTokenRepository;
import com.omnichannel.identity.repository.RoleRepository;
import com.omnichannel.identity.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User adminUser;
    private User customerUser;
    private Role adminRole;
    private Role customerRole;

    @BeforeEach
    void setUp() {
        adminRole = Role.builder().id(1L).name("ADMIN").description("Administrator").build();
        customerRole = Role.builder().id(2L).name("CUSTOMER").description("Customer").build();

        adminUser = User.builder()
                .id(UUID.randomUUID())
                .username("admin")
                .email("admin@example.com")
                .passwordHash("encodedAdminPass")
                .fullName("Admin User")
                .status("ACTIVE")
                .enabled(true)
                .roles(new HashSet<>(Set.of(adminRole)))
                .createdAt(Instant.now())
                .build();

        customerUser = User.builder()
                .id(UUID.randomUUID())
                .username("customer")
                .email("customer@example.com")
                .passwordHash("encodedCustomerPass")
                .fullName("Customer User")
                .status("ACTIVE")
                .enabled(true)
                .roles(new HashSet<>(Set.of(customerRole)))
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("getUserProfile should return profile for existing user")
    void testGetUserProfile_Success() {
        when(userRepository.findById(customerUser.getId())).thenReturn(Optional.of(customerUser));

        UserResponse response = userService.getUserProfile(customerUser.getId());

        assertNotNull(response);
        assertEquals(customerUser.getEmail(), response.getEmail());
        assertEquals("Customer User", response.getFullName());
    }

    @Test
    @DisplayName("updateProfile should update name and phone")
    void testUpdateProfile_Success() {
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .fullName("Updated Name")
                .phone("0999888777")
                .build();

        when(userRepository.findById(customerUser.getId())).thenReturn(Optional.of(customerUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArguments()[0]);

        UserResponse response = userService.updateProfile(customerUser.getId(), request);

        assertNotNull(response);
        assertEquals("Updated Name", response.getFullName());
        assertEquals("0999888777", response.getPhone());
    }

    @Test
    @DisplayName("changePassword should succeed when current password is correct")
    void testChangePassword_Success() {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("OldPassword123!")
                .newPassword("NewPassword123!")
                .build();

        when(userRepository.findById(customerUser.getId())).thenReturn(Optional.of(customerUser));
        when(passwordEncoder.matches("OldPassword123!", customerUser.getPasswordHash())).thenReturn(true);
        when(passwordEncoder.matches("NewPassword123!", customerUser.getPasswordHash())).thenReturn(false);
        when(passwordEncoder.encode("NewPassword123!")).thenReturn("newEncodedPass");

        MessageResponse response = userService.changePassword(customerUser.getId(), request);

        assertNotNull(response);
        assertEquals("Password changed successfully", response.getMessage());
        verify(refreshTokenRepository, times(1)).revokeAllByUserId(eq(customerUser.getId()), any());
    }

    @Test
    @DisplayName("changePassword should fail when current password is incorrect")
    void testChangePassword_WrongCurrentPassword_ThrowsBadRequest() {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("WrongPassword!")
                .newPassword("NewPassword123!")
                .build();

        when(userRepository.findById(customerUser.getId())).thenReturn(Optional.of(customerUser));
        when(passwordEncoder.matches("WrongPassword!", customerUser.getPasswordHash())).thenReturn(false);

        assertThrows(BadRequestException.class, () -> userService.changePassword(customerUser.getId(), request));
    }

    @Test
    @DisplayName("getAllUsers should return paginated users")
    void testGetAllUsers_Success() {
        Page<User> page = new PageImpl<>(List.of(adminUser, customerUser));
        when(userRepository.findAllWithFilters(any(), any(), any(), any(Pageable.class))).thenReturn(page);

        PageResponse<UserResponse> response = userService.getAllUsers(null, null, null, 0, 10);

        assertNotNull(response);
        assertEquals(2, response.getContent().size());
    }

    @Test
    @DisplayName("updateUserStatus should prevent disabling last remaining admin")
    void testUpdateUserStatus_DeactivateLastAdmin_ThrowsBadRequest() {
        UpdateStatusRequest request = UpdateStatusRequest.builder()
                .status("LOCKED")
                .enabled(false)
                .build();

        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRepository.countActiveAdmins()).thenReturn(1L);

        assertThrows(BadRequestException.class, () -> userService.updateUserStatus(adminUser.getId(), request));
    }

    @Test
    @DisplayName("updateUserRoles should prevent removing ADMIN role from last remaining admin")
    void testUpdateUserRoles_DemoteLastAdmin_ThrowsBadRequest() {
        UpdateRolesRequest request = UpdateRolesRequest.builder()
                .roles(Set.of("CUSTOMER"))
                .build();

        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRepository.countActiveAdmins()).thenReturn(1L);

        assertThrows(BadRequestException.class, () -> userService.updateUserRoles(adminUser.getId(), request));
    }
}
