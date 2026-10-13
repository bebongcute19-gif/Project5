package com.omnichannel.identity.controller;

import com.omnichannel.identity.dto.request.AddressRequest;
import com.omnichannel.identity.dto.request.LoginRequest;
import com.omnichannel.identity.dto.request.RefreshTokenRequest;
import com.omnichannel.identity.dto.request.RegisterRequest;
import com.omnichannel.identity.dto.response.AddressResponse;
import com.omnichannel.identity.dto.response.MessageResponse;
import com.omnichannel.identity.dto.response.TokenResponse;
import com.omnichannel.identity.dto.response.UserResponse;
import com.omnichannel.identity.security.UserPrincipal;
import com.omnichannel.identity.service.AddressService;
import com.omnichannel.identity.service.AuthService;
import com.omnichannel.identity.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final AddressService addressService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        TokenResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<TokenResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        TokenResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestBody(required = false) RefreshTokenRequest request) {
        String token = request != null ? request.getRefreshToken() : null;
        MessageResponse response = authService.logout(currentUser != null ? currentUser.getId() : null, token);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal UserPrincipal currentUser) {
        UserResponse response = userService.getUserProfile(currentUser.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/addresses")
    public ResponseEntity<List<AddressResponse>> getUserAddresses(@AuthenticationPrincipal UserPrincipal currentUser) {
        List<AddressResponse> response = addressService.getUserAddresses(currentUser.getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/addresses")
    public ResponseEntity<AddressResponse> addAddress(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody AddressRequest request) {
        AddressResponse response = addressService.addAddress(currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
