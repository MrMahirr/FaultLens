package com.faultlens.api.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Opaque, single-use refresh tokens kept in Redis (P07).
 *
 * <p>The browser holds the raw token in an HttpOnly cookie; Redis holds only its SHA-256, mapped
 * to the user id (ids survive a username change). Every refresh consumes the token atomically
 * ({@code GETDEL}) and issues a new one, so a stolen token works at most once and the legitimate
 * client notices (its next refresh fails). Each user also has a set of live token hashes so a
 * password change or reset can end every session.
 *
 * <p>Without Redis no refresh token is issued: the user keeps the 15-minute access token and logs
 * in again afterwards.
 */
@Slf4j
@Component
public class RefreshTokenStore {
    static final String TOKEN_PREFIX = "faultlens:refresh:";
    static final String USER_PREFIX = "faultlens:refresh-user:";
    private static final int TOKEN_BYTES = 32;

    private final StringRedisTemplate redis;
    private final Duration ttl;
    private final SecureRandom random = new SecureRandom();

    public RefreshTokenStore(StringRedisTemplate redis,
                             @Value("${faultlens.auth.refresh-ttl:P7D}") Duration ttl) {
        this.redis = redis;
        this.ttl = ttl;
    }

    public Duration ttl() {
        return ttl;
    }

    /** A new refresh token for the user, or empty when Redis is unavailable. */
    public Optional<String> issue(long userId) {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String hash = hash(token);
        try {
            redis.opsForValue().set(TOKEN_PREFIX + hash, Long.toString(userId), ttl);
            String userKey = USER_PREFIX + userId;
            redis.opsForSet().add(userKey, hash);
            redis.expire(userKey, ttl);
            return Optional.of(token);
        } catch (RuntimeException ex) {
            log.warn("Refresh tokens unavailable (Redis); the session ends with its access token", ex);
            return Optional.empty();
        }
    }

    /** Consumes the token: the owning user id if it was live, empty otherwise (single use). */
    public Optional<Long> consume(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        String hash = hash(token);
        try {
            String userId = redis.opsForValue().getAndDelete(TOKEN_PREFIX + hash);
            if (userId == null) {
                return Optional.empty();
            }
            redis.opsForSet().remove(USER_PREFIX + userId, hash);
            return Optional.of(Long.parseLong(userId));
        } catch (RuntimeException ex) {
            log.warn("Refresh tokens unavailable (Redis); refresh rejected", ex);
            return Optional.empty();
        }
    }

    /** Ends one session (logout). */
    public void revoke(String token) {
        consume(token);
    }

    /** Ends every session of the user (password change/reset, account deletion). */
    public void revokeAll(long userId) {
        String userKey = USER_PREFIX + userId;
        try {
            Set<String> hashes = redis.opsForSet().members(userKey);
            if (hashes != null && !hashes.isEmpty()) {
                redis.delete(hashes.stream().map(h -> TOKEN_PREFIX + h).toList());
            }
            redis.delete(userKey);
        } catch (RuntimeException ex) {
            log.warn("Could not revoke the refresh tokens of user {} (Redis)", userId, ex);
        }
    }

    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
