package com.faultlens.api.service;

import com.faultlens.api.dto.AuthDtos.ChangePasswordRequest;
import com.faultlens.api.dto.AuthDtos.LoginRequest;
import com.faultlens.api.dto.AuthDtos.ProfileResponse;
import com.faultlens.api.dto.AuthDtos.ProfileUpdateResponse;
import com.faultlens.api.dto.AuthDtos.TokenResponse;
import com.faultlens.api.dto.AuthDtos.UpdateProfileRequest;
import com.faultlens.api.model.UserAccount;
import com.faultlens.api.repository.UserAccountRepository;
import com.faultlens.api.security.JwtService;
import com.faultlens.api.security.LoginAttemptLimiter;
import com.faultlens.api.security.LoginLockedException;
import com.faultlens.api.security.RefreshTokenStore;
import com.faultlens.common.exception.ConflictException;
import com.faultlens.common.exception.FaultLensException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Login, token refresh and the caller's own account.
 *
 * <p>Sessions (P07): a short-lived access JWT ({@code faultlens.jwt.expiration-ms}, 15 minutes)
 * plus a single-use refresh token that the controller puts into an HttpOnly cookie. Refreshing
 * re-reads the user, so role changes and deletions take effect within one access-token lifetime.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {
    private final UserAccountRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenStore refreshTokens;
    private final LoginAttemptLimiter loginLimiter;

    /** Response body plus the refresh token for the cookie ({@code null}: none issued). */
    public record AuthSession(TokenResponse token, String refreshToken) {
    }

    /**
     * Authenticates a user. After too many failures the username is locked for a while, whether
     * or not the next password is right.
     */
    public AuthSession login(LoginRequest request) {
        String username = request.username();
        if (loginLimiter.isLocked(username)) {
            throw new LoginLockedException(loginLimiter.lockDuration());
        }
        UserAccount user = userRepository.findByUsername(username).orElse(null);
        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            loginLimiter.recordFailure(username);
            throw new BadCredentialsException("Invalid username or password");
        }
        loginLimiter.reset(username);
        return sessionFor(user);
    }

    /**
     * Exchanges a refresh token for a new access token and a new refresh token (rotation).
     */
    public AuthSession refresh(String refreshToken) {
        Long userId = refreshTokens.consume(refreshToken)
                .orElseThrow(() -> new BadCredentialsException("Session expired; please log in again"));
        UserAccount user = userRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("Session expired; please log in again"));
        return sessionFor(user);
    }

    /** Ends the session of this refresh token (idempotent). */
    public void logout(String refreshToken) {
        refreshTokens.revoke(refreshToken);
    }

    /**
     * Returns the profile of the authenticated user.
     */
    public ProfileResponse getProfile(String username) {
        UserAccount user = findUserByUsername(username);
        return new ProfileResponse(user.getUsername(), user.getEmail(), user.getRole());
    }

    /**
     * Updates the profile (username, email) of the authenticated user.
     */
    @Transactional
    public ProfileUpdateResponse updateProfile(String currentUsername, UpdateProfileRequest request) {
        UserAccount user = findUserByUsername(currentUsername);
        String newUsername = request.username().trim();
        if (!newUsername.equals(user.getUsername()) && userRepository.findByUsername(newUsername).isPresent()) {
            throw new ConflictException("Username is already taken", "USERNAME_TAKEN");
        }
        user.setUsername(newUsername);
        user.setEmail(request.email());
        UserAccount saved;
        try {
            saved = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            // taken concurrently between the check and the insert (unique constraint)
            throw new ConflictException("Username is already taken", "USERNAME_TAKEN");
        }
        // The JWT subject is the username: after a rename the caller's token points at nobody.
        // (Refresh tokens map to the user id and keep working.)
        TokenResponse token = accessTokenFor(saved);
        return new ProfileUpdateResponse(saved.getUsername(), saved.getEmail(), saved.getRole(),
                token.token(), token.expiresAt());
    }

    /**
     * Changes the password of the authenticated user, clears the password-change requirement and
     * ends every other session (a fresh refresh token is issued for this one).
     *
     * <p>A wrong current password is reported as 400 (not 401): the caller is authenticated, and a
     * 401 would make the frontend log the user out.
     */
    @Transactional
    public AuthSession changePassword(String username, ChangePasswordRequest request) {
        UserAccount user = findUserByUsername(username);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new FaultLensException("Current password is incorrect", "INVALID_CURRENT_PASSWORD");
        }
        if (request.newPassword().equals(request.currentPassword())) {
            throw new FaultLensException("New password must differ from the current password", "PASSWORD_REUSED");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setMustChangePassword(false);
        UserAccount saved = userRepository.save(user);
        if (saved.getId() != null) {
            refreshTokens.revokeAll(saved.getId());
        }
        return sessionFor(saved);
    }

    private UserAccount findUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("User not found"));
    }

    private AuthSession sessionFor(UserAccount user) {
        String refresh = user.getId() == null ? null : refreshTokens.issue(user.getId()).orElse(null);
        return new AuthSession(accessTokenFor(user), refresh);
    }

    private TokenResponse accessTokenFor(UserAccount user) {
        String token = jwtService.generateToken(
                user.getUsername(), List.of(user.getRole()), user.isMustChangePassword());
        return new TokenResponse(token, jwtService.expiresAt(), user.isMustChangePassword());
    }
}
