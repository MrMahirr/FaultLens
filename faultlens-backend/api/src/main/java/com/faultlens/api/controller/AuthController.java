package com.faultlens.api.controller;

import com.faultlens.api.dto.AuthDtos.ChangePasswordRequest;
import com.faultlens.api.dto.AuthDtos.LoginRequest;
import com.faultlens.api.dto.AuthDtos.ProfileResponse;
import com.faultlens.api.dto.AuthDtos.RefreshRequest;
import com.faultlens.api.dto.AuthDtos.TokenResponse;
import com.faultlens.api.dto.AuthDtos.UpdateProfileRequest;
import com.faultlens.api.service.AuthService;
import com.faultlens.common.dto.ApiResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    /**
     * Authenticates a user and returns a JWT.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(request)));
    }

    /**
     * Refreshes a valid JWT.
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.refresh(request)));
    }

    /**
     * Returns the profile of the authenticated user.
     */
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile(Principal principal) {
        return ResponseEntity.ok(ApiResponse.ok(authService.getProfile(principal.getName())));
    }

    /**
     * Updates the profile of the authenticated user.
     */
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(
            Principal principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.updateProfile(principal.getName(), request)));
    }

    /**
     * Changes the password of the authenticated user.
     */
    @PutMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            Principal principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(principal.getName(), request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
