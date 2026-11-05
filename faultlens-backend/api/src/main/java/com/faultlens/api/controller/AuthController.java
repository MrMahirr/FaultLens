package com.faultlens.api.controller;

import com.faultlens.api.dto.AuthDtos.ChangePasswordRequest;
import com.faultlens.api.dto.AuthDtos.LoginRequest;
import com.faultlens.api.dto.AuthDtos.ProfileResponse;
import com.faultlens.api.dto.AuthDtos.ProfileUpdateResponse;
import com.faultlens.api.dto.AuthDtos.TokenResponse;
import com.faultlens.api.dto.AuthDtos.UpdateProfileRequest;
import com.faultlens.api.service.AuthService;
import com.faultlens.api.service.AuthService.AuthSession;
import com.faultlens.common.dto.ApiResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication. The access JWT travels in the response body (the SPA sends it as a bearer
 * header); the refresh token only ever travels in an HttpOnly, SameSite=Strict cookie scoped to
 * {@code /api/v1/auth}, so page scripts cannot read it.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    public static final String REFRESH_COOKIE = "faultlens_refresh";
    static final String COOKIE_PATH = "/api/v1/auth";

    private final AuthService authService;
    private final boolean secureCookie;
    private final Duration refreshTtl;

    public AuthController(AuthService authService,
                          @Value("${faultlens.auth.refresh-cookie-secure:true}") boolean secureCookie,
                          @Value("${faultlens.auth.refresh-ttl:P7D}") Duration refreshTtl) {
        this.authService = authService;
        this.secureCookie = secureCookie;
        this.refreshTtl = refreshTtl;
    }

    /**
     * Authenticates a user: access token in the body, refresh token in the cookie.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        return withSession(authService.login(request));
    }

    /**
     * Rotates the refresh cookie and returns a new access token.
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        return withSession(authService.refresh(refreshToken));
    }

    /**
     * Revokes the refresh token and clears the cookie. Idempotent; needs no access token.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO)).build();
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
    public ResponseEntity<ApiResponse<ProfileUpdateResponse>> updateProfile(
            Principal principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.updateProfile(principal.getName(), request)));
    }

    /**
     * Changes the password of the authenticated user and returns a fresh token (without the
     * password-change restriction). Other sessions of the user end.
     */
    @PutMapping("/password")
    public ResponseEntity<ApiResponse<TokenResponse>> changePassword(
            Principal principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        return withSession(authService.changePassword(principal.getName(), request));
    }

    private ResponseEntity<ApiResponse<TokenResponse>> withSession(AuthSession session) {
        ResponseEntity.BodyBuilder response = ResponseEntity.ok();
        if (session.refreshToken() != null) {
            response.header(HttpHeaders.SET_COOKIE, cookie(session.refreshToken(), refreshTtl));
        }
        return response.body(ApiResponse.ok(session.token()));
    }

    private String cookie(String value, Duration maxAge) {
        return ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Strict")
                .path(COOKIE_PATH)
                .maxAge(maxAge)
                .build()
                .toString();
    }
}
