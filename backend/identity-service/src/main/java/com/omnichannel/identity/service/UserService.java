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
import com.omnichannel.identity.exception.ResourceNotFoundException;
import com.omnichannel.identity.repository.RefreshTokenRepository;
import com.omnichannel.identity.repository.RoleRepository;
import com.omnichannel.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserResponse getUserProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return AuthService.mapToUserResponse(user);
    }

    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName().trim());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone().trim());
        }

        User updatedUser = userRepository.save(user);
        return AuthService.mapToUserResponse(updatedUser);
    }

    @Transactional
    public MessageResponse changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password does not match");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new BadRequestException("New password must be different from current password");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Revoke all refresh tokens on password change
        refreshTokenRepository.revokeAllByUserId(userId, Instant.now());

        return MessageResponse.builder().message("Password changed successfully").build();
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getAllUsers(String email, String username, String status, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by("createdAt").descending());
        Page<User> userPage = userRepository.findAllWithFilters(
                (email != null && !email.isBlank()) ? email.trim() : null,
                (username != null && !username.isBlank()) ? username.trim() : null,
                (status != null && !status.isBlank()) ? status.trim() : null,
                pageable
        );

        Page<UserResponse> dtoPage = userPage.map(AuthService::mapToUserResponse);
        return PageResponse.of(dtoPage);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return AuthService.mapToUserResponse(user);
    }

    @Transactional
    public UserResponse updateUserStatus(UUID targetUserId, UpdateStatusRequest request) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + targetUserId));

        boolean isUserAdmin = user.getRoles().stream()
                .anyMatch(r -> "ADMIN".equalsIgnoreCase(r.getName()) || "ROLE_ADMIN".equalsIgnoreCase(r.getName()));

        boolean willBeDisabled = (request.getEnabled() != null && !request.getEnabled())
                || (request.getStatus() != null && !"ACTIVE".equalsIgnoreCase(request.getStatus()));

        if (isUserAdmin && willBeDisabled) {
            long activeAdmins = userRepository.countActiveAdmins();
            if (activeAdmins <= 1 && Boolean.TRUE.equals(user.getEnabled()) && "ACTIVE".equalsIgnoreCase(user.getStatus())) {
                throw new BadRequestException("Cannot deactivate or lock the last remaining administrator account");
            }
        }

        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            user.setStatus(request.getStatus().toUpperCase());
        }
        if (request.getEnabled() != null) {
            user.setEnabled(request.getEnabled());
        }

        if (willBeDisabled) {
            refreshTokenRepository.revokeAllByUserId(targetUserId, Instant.now());
        }

        User updatedUser = userRepository.save(user);
        return AuthService.mapToUserResponse(updatedUser);
    }

    @Transactional
    public UserResponse updateUserRoles(UUID targetUserId, UpdateRolesRequest request) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + targetUserId));

        boolean wasAdmin = user.getRoles().stream()
                .anyMatch(r -> "ADMIN".equalsIgnoreCase(r.getName()) || "ROLE_ADMIN".equalsIgnoreCase(r.getName()));

        boolean willBeAdmin = request.getRoles().stream()
                .anyMatch(r -> "ADMIN".equalsIgnoreCase(r) || "ROLE_ADMIN".equalsIgnoreCase(r));

        if (wasAdmin && !willBeAdmin) {
            long activeAdmins = userRepository.countActiveAdmins();
            if (activeAdmins <= 1 && Boolean.TRUE.equals(user.getEnabled()) && "ACTIVE".equalsIgnoreCase(user.getStatus())) {
                throw new BadRequestException("Cannot remove administrator role from the last remaining administrator");
            }
        }

        Set<Role> newRoles = new HashSet<>();
        for (String roleName : request.getRoles()) {
            String normalizedRole = roleName.toUpperCase().replace("ROLE_", "");
            Role role = roleRepository.findByName(normalizedRole)
                    .orElseGet(() -> roleRepository.save(Role.builder().name(normalizedRole).description(normalizedRole).build()));
            newRoles.add(role);
        }

        user.setRoles(newRoles);
        User updatedUser = userRepository.save(user);
        return AuthService.mapToUserResponse(updatedUser);
    }
}
