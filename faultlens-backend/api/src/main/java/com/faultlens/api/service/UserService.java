package com.faultlens.api.service;

import com.faultlens.api.dto.UserDtos.CreateUserRequest;
import com.faultlens.api.dto.UserDtos.UserResponse;
import com.faultlens.api.model.UserAccount;
import com.faultlens.api.repository.UserAccountRepository;
import com.faultlens.api.security.RefreshTokenStore;
import com.faultlens.api.security.Roles;
import com.faultlens.common.exception.ConflictException;
import com.faultlens.common.exception.FaultLensException;
import com.faultlens.common.exception.ResourceNotFoundException;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * User management for admins.
 *
 * <p>Guards against locking everyone out: an admin cannot delete or re-role themselves, and the
 * last admin can be neither demoted nor deleted. Accounts created or reset here must change their
 * password at the next login; a reset or deletion also ends the user's sessions (refresh tokens).
 * A role change takes effect at the user's next token refresh (within 15 minutes).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private final UserAccountRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenStore refreshTokens;

    public List<UserResponse> list() {
        return repository.findAllByOrderByUsernameAsc().stream().map(UserService::toResponse).toList();
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        requireValidRole(request.role());
        String username = request.username().trim();
        if (repository.findByUsername(username).isPresent()) {
            throw new ConflictException("Username is already taken", "USERNAME_TAKEN");
        }
        UserAccount user = UserAccount.builder()
                .username(username)
                .email(blankToNull(request.email()))
                .role(request.role())
                .password(passwordEncoder.encode(request.password()))
                .mustChangePassword(true)
                .createdAt(Instant.now())
                .build();
        try {
            return toResponse(repository.saveAndFlush(user));
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("Username is already taken", "USERNAME_TAKEN");
        }
    }

    @Transactional
    public UserResponse changeRole(Long id, String role, String actingUsername) {
        requireValidRole(role);
        UserAccount user = find(id);
        if (user.getUsername().equals(actingUsername)) {
            throw new FaultLensException("You cannot change your own role", "CANNOT_CHANGE_OWN_ROLE");
        }
        if (Roles.ADMIN.equals(user.getRole()) && !Roles.ADMIN.equals(role)) {
            requireAnotherAdmin();
        }
        user.setRole(role);
        return toResponse(repository.save(user));
    }

    @Transactional
    public UserResponse resetPassword(Long id, String password) {
        UserAccount user = find(id);
        user.setPassword(passwordEncoder.encode(password));
        user.setMustChangePassword(true);
        UserAccount saved = repository.save(user);
        refreshTokens.revokeAll(saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id, String actingUsername) {
        UserAccount user = find(id);
        if (user.getUsername().equals(actingUsername)) {
            throw new FaultLensException("You cannot delete your own account", "CANNOT_DELETE_SELF");
        }
        if (Roles.ADMIN.equals(user.getRole())) {
            requireAnotherAdmin();
        }
        repository.delete(user);
        refreshTokens.revokeAll(user.getId());
    }

    private void requireAnotherAdmin() {
        if (repository.countByRole(Roles.ADMIN) <= 1) {
            throw new FaultLensException("At least one admin must remain", "LAST_ADMIN");
        }
    }

    private static void requireValidRole(String role) {
        if (!Roles.isValid(role)) {
            throw new FaultLensException("Role must be ADMIN, EDITOR or VIEWER", "INVALID_ROLE");
        }
    }

    private UserAccount find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found", "USER_NOT_FOUND"));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    static UserResponse toResponse(UserAccount user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRole(),
                user.isMustChangePassword(), user.getCreatedAt());
    }
}
