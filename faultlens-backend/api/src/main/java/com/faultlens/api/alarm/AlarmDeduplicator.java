package com.faultlens.api.alarm;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * One alarm per key per cooldown, across instances ({@code SET NX EX}). Fails open: without
 * Redis the alarm is raised (a duplicate is better than a lost alarm).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlarmDeduplicator {
    static final String KEY_PREFIX = "faultlens:alarm-dedup:";

    private final StringRedisTemplate redis;

    /**
     * True if an alarm for {@code key} may be raised now (and starts its cooldown). A cooldown of
     * zero means no deduplication.
     */
    public boolean tryAcquire(String key, Duration cooldown) {
        if (cooldown == null || cooldown.isZero() || cooldown.isNegative()) {
            return true;
        }
        try {
            return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(KEY_PREFIX + key, "1", cooldown));
        } catch (RuntimeException ex) {
            log.warn("Alarm dedup unavailable (Redis); raising the alarm for {}", key, ex);
            return true;
        }
    }
}
