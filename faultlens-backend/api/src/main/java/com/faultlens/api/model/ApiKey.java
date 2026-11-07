package com.faultlens.api.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Scoped service token (P10 / 1.1). The token itself is never stored, only its SHA-256.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "api_keys")
public class ApiKey {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String prefix;
    private String secretHash;
    /** Comma-separated {@code ApiKeyScope} values. */
    private String scopes;
    private String createdBy;
    private Instant createdAt;
    private Instant expiresAt;
    private Instant lastUsedAt;
    private String lastUsedIp;
    private Instant revokedAt;

    public boolean isActive(Instant now) {
        return revokedAt == null && (expiresAt == null || expiresAt.isAfter(now));
    }
}
