package com.faultlens.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * API key management (ADMIN only). The token is returned once, at creation.
 */
public final class ApiKeyDtos {
    private ApiKeyDtos() {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ApiKeyResponse(Long id, String name, String displayKey, List<String> scopes,
                                 String createdBy, Instant createdAt, Instant expiresAt,
                                 Instant lastUsedAt, String lastUsedIp, Instant revokedAt, boolean active) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record CreateApiKeyRequest(
            @NotBlank @Size(max = 100) String name,
            @NotEmpty(message = "At least one scope is required") List<@NotBlank String> scopes,
            /* null = never expires */
            @Min(1) @Max(3650) Integer expiresInDays) {
    }

    /** Creation result: {@code token} is the only time the full key is visible. */
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record CreatedApiKeyResponse(ApiKeyResponse key, String token) {
    }
}
