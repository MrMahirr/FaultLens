package com.faultlens.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * User management (ADMIN only).
 */
public final class UserDtos {
    private static final String ROLE_PATTERN = "ADMIN|EDITOR|VIEWER";
    private static final String ROLE_MESSAGE = "Role must be ADMIN, EDITOR or VIEWER";

    private UserDtos() {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record UserResponse(Long id, String username, String email, String role,
                               boolean mustChangePassword, Instant createdAt) {
    }

    /** New accounts must change the initial password at first login. */
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record CreateUserRequest(
            @NotBlank @Size(max = 100) String username,
            @Email @Size(max = 255) String email,
            @NotBlank @Pattern(regexp = ROLE_PATTERN, message = ROLE_MESSAGE) String role,
            @NotBlank @Size(min = 8, max = 128, message = "Password must be 8-128 characters") String password) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record UpdateRoleRequest(@NotBlank @Pattern(regexp = ROLE_PATTERN, message = ROLE_MESSAGE) String role) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ResetPasswordRequest(
            @NotBlank @Size(min = 8, max = 128, message = "Password must be 8-128 characters") String password) {
    }
}
