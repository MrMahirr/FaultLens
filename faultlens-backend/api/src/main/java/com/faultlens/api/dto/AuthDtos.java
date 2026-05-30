package com.faultlens.api.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

public final class AuthDtos {
    private AuthDtos() {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record RefreshRequest(@NotBlank String token) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record TokenResponse(String token, Instant expiresAt) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ProfileResponse(String username, String email, String role) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record UpdateProfileRequest(@NotBlank String username, String email) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank String newPassword) {
    }
}
