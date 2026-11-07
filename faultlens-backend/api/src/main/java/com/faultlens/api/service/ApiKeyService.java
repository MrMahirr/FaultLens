package com.faultlens.api.service;

import com.faultlens.api.dto.ApiKeyDtos.ApiKeyResponse;
import com.faultlens.api.dto.ApiKeyDtos.CreateApiKeyRequest;
import com.faultlens.api.dto.ApiKeyDtos.CreatedApiKeyResponse;
import com.faultlens.api.model.ApiKey;
import com.faultlens.collector.model.LogSource;
import com.faultlens.collector.repository.LogSourceRepository;
import com.faultlens.common.enums.LogSourceType;
import com.faultlens.api.repository.ApiKeyRepository;
import com.faultlens.api.security.ApiKeyScope;
import com.faultlens.api.security.ApiKeyTokens;
import com.faultlens.common.exception.FaultLensException;
import com.faultlens.common.exception.ResourceNotFoundException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates, lists, revokes and verifies API keys (P10 / 1.1).
 *
 * <p>Revoking keeps the row (who created it, when it was last used) so leaks can be traced; a
 * revoked or expired key is rejected at once. The last-use stamp is written at most once a
 * minute per key and instance, not on every request.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class ApiKeyService {
    static final Duration USAGE_WRITE_INTERVAL = Duration.ofMinutes(1);

    private final ApiKeyRepository repository;
    private final LogSourceRepository sources;
    private final Map<Long, Instant> lastUsageWrite = new ConcurrentHashMap<>();

    public ApiKeyService(ApiKeyRepository repository, LogSourceRepository sources) {
        this.repository = repository;
        this.sources = sources;
    }

    /** An authenticated key: what the security filter needs. */
    public record VerifiedKey(Long id, String name, Set<ApiKeyScope> scopes, Long sourceId) {
    }

    public List<ApiKeyResponse> list() {
        Instant now = Instant.now();
        return repository.findAllByOrderByCreatedAtDesc().stream().map(k -> toResponse(k, now)).toList();
    }

    @Transactional
    public CreatedApiKeyResponse create(CreateApiKeyRequest request, String createdBy) {
        Set<ApiKeyScope> scopes = parseScopes(request.scopes());
        Long sourceId = requireIngestSource(scopes, request.sourceId());
        ApiKeyTokens.Generated generated = ApiKeyTokens.generate();
        Instant now = Instant.now();
        ApiKey key = ApiKey.builder()
                .name(request.name().trim())
                .prefix(generated.prefix())
                .secretHash(generated.hash())
                .scopes(join(scopes))
                .sourceId(sourceId)
                .createdBy(createdBy)
                .createdAt(now)
                .expiresAt(request.expiresInDays() == null ? null : now.plus(Duration.ofDays(request.expiresInDays())))
                .build();
        ApiKey saved = repository.saveAndFlush(key);
        log.info("API key {} ({}) created by {} with scopes {}", saved.getId(), saved.getName(), createdBy, saved.getScopes());
        return new CreatedApiKeyResponse(toResponse(saved, now), generated.token());
    }

    /** Revokes a key; revoking twice is a no-op. */
    @Transactional
    public void revoke(Long id, String revokedBy) {
        ApiKey key = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("API key not found", "API_KEY_NOT_FOUND"));
        if (key.getRevokedAt() == null) {
            key.setRevokedAt(Instant.now());
            log.info("API key {} ({}) revoked by {}", key.getId(), key.getName(), revokedBy);
        }
        lastUsageWrite.remove(id);
    }

    /** Verifies a presented token; empty for unknown, malformed, revoked or expired keys. */
    public Optional<VerifiedKey> verify(String token) {
        Instant now = Instant.now();
        return ApiKeyTokens.prefixOf(token)
                .flatMap(repository::findByPrefix)
                .filter(key -> key.isActive(now))
                .filter(key -> ApiKeyTokens.matches(token, key.getSecretHash()))
                .map(key -> new VerifiedKey(key.getId(), key.getName(), storedScopes(key.getScopes()), key.getSourceId()));
    }

    /**
     * Records use; throttled so a busy CI does not turn every request into a write. Runs outside
     * the class-level read-only transaction: the update commits on its own, and a failure is only
     * logged (it must never fail the request).
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void recordUse(Long id, String ip) {
        Instant now = Instant.now();
        Instant previous = lastUsageWrite.get(id);
        if (previous != null && previous.plus(USAGE_WRITE_INTERVAL).isAfter(now)) {
            return;
        }
        lastUsageWrite.put(id, now);
        try {
            repository.markUsed(id, now, ip);
        } catch (RuntimeException ex) {
            log.warn("Could not record API key use", ex);
        }
    }

    /**
     * A logs:ingest key needs an HTTP source; other keys have none (a source on them would
     * suggest a binding that does nothing).
     */
    private Long requireIngestSource(Set<ApiKeyScope> scopes, Long sourceId) {
        if (!scopes.contains(ApiKeyScope.LOGS_INGEST)) {
            if (sourceId != null) {
                throw new FaultLensException("source_id is only used with the logs:ingest scope", "INVALID_API_KEY_SOURCE");
            }
            return null;
        }
        if (sourceId == null) {
            throw new FaultLensException("A logs:ingest key needs source_id (an HTTP source)", "INVALID_API_KEY_SOURCE");
        }
        LogSource source = sources.findById(sourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Source not found", "SOURCE_NOT_FOUND"));
        if (source.getType() != LogSourceType.HTTP) {
            throw new FaultLensException("Logs can only be pushed to an HTTP source", "INVALID_API_KEY_SOURCE");
        }
        return sourceId;
    }

    static Set<ApiKeyScope> parseScopes(Collection<String> values) {
        Set<ApiKeyScope> scopes = EnumSet.noneOf(ApiKeyScope.class);
        for (String value : values) {
            scopes.add(ApiKeyScope.fromValue(value.trim()).orElseThrow(() -> new FaultLensException(
                    "Unknown scope: " + value + " (allowed: " + allowedScopes() + ")", "INVALID_SCOPE")));
        }
        return scopes;
    }

    private static Set<ApiKeyScope> storedScopes(String stored) {
        Set<ApiKeyScope> scopes = EnumSet.noneOf(ApiKeyScope.class);
        Arrays.stream(stored.split(",")).map(String::trim).map(ApiKeyScope::fromValue)
                .flatMap(Optional::stream).forEach(scopes::add);
        return scopes;
    }

    private static String allowedScopes() {
        return Arrays.stream(ApiKeyScope.values()).map(ApiKeyScope::value).collect(Collectors.joining(", "));
    }

    private static String join(Set<ApiKeyScope> scopes) {
        return scopes.stream().map(ApiKeyScope::value).collect(Collectors.joining(","));
    }

    private static ApiKeyResponse toResponse(ApiKey key, Instant now) {
        return new ApiKeyResponse(key.getId(), key.getName(), ApiKeyTokens.display(key.getPrefix()),
                storedScopes(key.getScopes()).stream().map(ApiKeyScope::value).toList(), key.getSourceId(),
                key.getCreatedBy(), key.getCreatedAt(), key.getExpiresAt(), key.getLastUsedAt(),
                key.getLastUsedIp(), key.getRevokedAt(), key.isActive(now));
    }
}
