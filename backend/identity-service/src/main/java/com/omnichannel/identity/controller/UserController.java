package com.omnichannel.identity.controller;

import com.omnichannel.identity.dto.request.*;
import com.omnichannel.identity.dto.response.AddressResponse;
import com.omnichannel.identity.dto.response.MessageResponse;
import com.omnichannel.identity.dto.response.PageResponse;
import com.omnichannel.identity.dto.response.UserResponse;
import com.omnichannel.identity.security.UserPrincipal;
import com.omnichannel.identity.service.AddressService;
import com.omnichannel.identity.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AddressService addressService;

    // --- Current User Profile Endpoints ---

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUserProfile(@AuthenticationPrincipal UserPrincipal currentUser) {
        UserResponse response = userService.getUserProfile(currentUser.getId());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateCurrentUserProfile(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody UpdateProfileRequest request) {
        UserResponse response = userService.updateProfile(currentUser.getId(), request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me/password")
    public ResponseEntity<MessageResponse> changePassword(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody ChangePasswordRequest request) {
        MessageResponse response = userService.changePassword(currentUser.getId(), request);
        return ResponseEntity.ok(response);
    }

    // --- Current User Addresses Endpoints ---

    @GetMapping("/me/addresses")
    public ResponseEntity<List<AddressResponse>> getCurrentUserAddresses(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        List<AddressResponse> response = addressService.getUserAddresses(currentUser.getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/me/addresses")
    public ResponseEntity<AddressResponse> addCurrentUserAddress(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody AddressRequest request) {
        AddressResponse response = addressService.addAddress(currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/me/addresses/{id}")
    public ResponseEntity<AddressResponse> updateCurrentUserAddress(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable("id") UUID addressId,
            @Valid @RequestBody AddressRequest request) {
        AddressResponse response = addressService.updateAddress(currentUser.getId(), addressId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/me/addresses/{id}")
    public ResponseEntity<Void> deleteCurrentUserAddress(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable("id") UUID addressId) {
        addressService.deleteAddress(currentUser.getId(), addressId);
        return ResponseEntity.noContent().build();
    }

    // --- Admin User Management Endpoints ---

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<PageResponse<UserResponse>> getAllUsers(
            @RequestParam(name = "email", required = false) String email,
            @RequestParam(name = "username", required = false) String username,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        PageResponse<UserResponse> response = userService.getAllUsers(email, username, status, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<UserResponse> getUserById(@PathVariable("id") UUID id) {
        UserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<UserResponse> updateUserStatus(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateStatusRequest request) {
        UserResponse response = userService.updateUserStatus(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/roles")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<UserResponse> updateUserRoles(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateRolesRequest request) {
        UserResponse response = userService.updateUserRoles(id, request);
        return ResponseEntity.ok(response);
    }
}
