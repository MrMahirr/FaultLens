package com.faultlens.api.security;

import java.time.Duration;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Requests per API key per minute, across instances (Redis {@code INCR} on a per-minute key).
 * Fails open like {@link LoginAttemptLimiter}: without Redis keys are not rate limited.
 */
@Slf4j
@Component
public class ApiKeyRateLimiter {
    static final String KEY_PREFIX = "faultlens:api-key-rate:";
    static final Duration WINDOW = Duration.ofMinutes(1);

    private final StringRedisTemplate redis;
    private final long limitPerMinute;

    public ApiKeyRateLimiter(StringRedisTemplate redis,
                             @Value("${faultlens.api-keys.rate-limit-per-minute:600}") long limitPerMinute) {
        this.redis = redis;
        this.limitPerMinute = limitPerMinute;
    }

    /** Counts this request; false when the key is over its limit for the current minute. */
    public boolean tryAcquire(Long keyId) {
        if (limitPerMinute <= 0) {
            return true;
        }
        String key = KEY_PREFIX + keyId + ":" + Instant.now().getEpochSecond() / WINDOW.toSeconds();
        try {
            Long count = redis.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redis.expire(key, WINDOW.multipliedBy(2));
            }
            return count == null || count <= limitPerMinute;
        } catch (RuntimeException ex) {
            log.warn("API key rate limiting unavailable (Redis)", ex);
            return true;
        }
    }

    /** Seconds until the current window ends (for Retry-After). */
    public long retryAfterSeconds() {
        long window = WINDOW.toSeconds();
        return window - Instant.now().getEpochSecond() % window;
    }
}
