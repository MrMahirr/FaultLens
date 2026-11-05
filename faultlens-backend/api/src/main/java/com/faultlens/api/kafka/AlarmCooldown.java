package com.faultlens.api.kafka;

import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Temporary alarm-storm guard until the P07 rule engine: at most one alarm per log group per
 * cooldown window (default 15 min), across instances ({@code SET key NX EX}). Analyses still
 * arrive and are stored; only the alarm and its notifications are suppressed. When Redis is
 * unavailable the alarm is raised (better a duplicate than a lost alarm).
 */
@Slf4j
@Component
public class AlarmCooldown {
    static final String KEY_PREFIX = "faultlens:alarm:group:";

    private final StringRedisTemplate redis;
    private final Duration window;

    public AlarmCooldown(StringRedisTemplate redis,
                         @Value("${faultlens.alarms.cooldown-minutes:15}") long cooldownMinutes) {
        this.redis = redis;
        this.window = Duration.ofMinutes(Math.max(1, cooldownMinutes));
    }

    /**
     * True if an alarm may be raised for the group now (and starts its cooldown).
     */
    public boolean tryAcquire(Long logGroupId) {
        if (logGroupId == null) {
            return true;
        }
        try {
            return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(KEY_PREFIX + logGroupId, "1", window));
        } catch (RuntimeException ex) {
            log.warn("Alarm cooldown unavailable (Redis); raising the alarm", ex);
            return true;
        }
    }

    Duration window() {
        return window;
    }
}
