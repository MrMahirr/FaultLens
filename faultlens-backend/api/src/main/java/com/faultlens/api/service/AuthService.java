package com.faultlens.api.service;

import com.faultlens.api.dto.AuthDtos.ChangePasswordRequest;
import com.faultlens.api.dto.AuthDtos.LoginRequest;
import com.faultlens.api.dto.AuthDtos.ProfileResponse;
import com.faultlens.api.dto.AuthDtos.ProfileUpdateResponse;
import com.faultlens.api.dto.AuthDtos.RefreshRequest;
import com.faultlens.api.dto.AuthDtos.TokenResponse;
import com.faultlens.api.dto.AuthDtos.UpdateProfileRequest;
import com.faultlens.api.model.UserAccount;
import com.faultlens.api.repository.UserAccountRepository;
import com.faultlens.api.security.JwtService;
import com.faultlens.common.exception.ConflictException;
import com.faultlens.common.exception.FaultLensException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {
    private final UserAccountRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * Authenticates a user and issues a JWT.
     */
    public TokenResponse login(LoginRequest request) {
        UserAccount user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException("Invalid username or password");
        }
        return tokenFor(user);
    }

    /**
     * Refreshes a valid JWT.
     */
    public TokenResponse refresh(RefreshRequest request) {
        if (!jwtService.validateToken(request.token())) {
            throw new BadCredentialsException("Invalid token");
        }
        String username = jwtService.extractUsername(request.token());
        UserAccount user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("Invalid token subject"));
        return tokenFor(user);
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
        TokenResponse token = tokenFor(saved);
        return new ProfileUpdateResponse(saved.getUsername(), saved.getEmail(), saved.getRole(),
                token.token(), token.expiresAt());
    }

    /**
     * Changes the password of the authenticated user and clears the password-change requirement.
     *
     * <p>A wrong current password is reported as 400 (not 401): the caller is authenticated, and a
     * 401 would make the frontend log the user out.
     */
    @Transactional
    public TokenResponse changePassword(String username, ChangePasswordRequest request) {
        UserAccount user = findUserByUsername(username);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new FaultLensException("Current password is incorrect", "INVALID_CURRENT_PASSWORD");
        }
        if (request.newPassword().equals(request.currentPassword())) {
            throw new FaultLensException("New password must differ from the current password", "PASSWORD_REUSED");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setMustChangePassword(false);
        return tokenFor(userRepository.save(user));
    }

    private UserAccount findUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("User not found"));
    }

    private TokenResponse tokenFor(UserAccount user) {
        String token = jwtService.generateToken(
                user.getUsername(), List.of(user.getRole()), user.isMustChangePassword());
        return new TokenResponse(token, jwtService.expiresAt(), user.isMustChangePassword());
    }
}
