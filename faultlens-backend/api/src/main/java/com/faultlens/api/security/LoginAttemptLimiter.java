package com.faultlens.api.security;

import java.time.Duration;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Locks a username for a while after repeated failed logins (P07: 5 failures → 15 minutes).
 *
 * <p>Counted per username, across instances (Redis {@code INCR} with a TTL set on the first
 * failure, so the lock ends {@code lockDuration} after the first failure of a burst). A successful
 * login clears the counter. Fails open: without Redis logins are not rate limited.
 */
@Slf4j
@Component
public class LoginAttemptLimiter {
    static final String KEY_PREFIX = "faultlens:login-failures:";

    private final StringRedisTemplate redis;
    private final int maxFailures;
    private final Duration lockDuration;

    public LoginAttemptLimiter(StringRedisTemplate redis,
                               @Value("${faultlens.auth.login-max-failures:5}") int maxFailures,
                               @Value("${faultlens.auth.login-lock-duration:PT15M}") Duration lockDuration) {
        this.redis = redis;
        this.maxFailures = maxFailures;
        this.lockDuration = lockDuration;
    }

    public Duration lockDuration() {
        return lockDuration;
    }

    public boolean isLocked(String username) {
        try {
            String failures = redis.opsForValue().get(key(username));
            return failures != null && Long.parseLong(failures) >= maxFailures;
        } catch (RuntimeException ex) {
            log.warn("Login rate limiting unavailable (Redis)", ex);
            return false;
        }
    }

    public void recordFailure(String username) {
        try {
            Long failures = redis.opsForValue().increment(key(username));
            if (failures != null && failures == 1L) {
                redis.expire(key(username), lockDuration);
            }
        } catch (RuntimeException ex) {
            log.warn("Login rate limiting unavailable (Redis)", ex);
        }
    }

    public void reset(String username) {
        try {
            redis.delete(key(username));
        } catch (RuntimeException ex) {
            log.warn("Login rate limiting unavailable (Redis)", ex);
        }
    }

    private static String key(String username) {
        return KEY_PREFIX + (username == null ? "" : username.trim().toLowerCase(Locale.ROOT));
    }
}
