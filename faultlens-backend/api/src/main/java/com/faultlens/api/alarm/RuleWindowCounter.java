package com.faultlens.api.alarm;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Sliding-window counts for THRESHOLD rules (ADR 0001): one Redis counter per rule and minute;
 * the count over a window is the sum of its minute buckets. One-minute precision, one INCR per
 * matching entry, shared by all instances.
 */
@Slf4j
@Component
public class RuleWindowCounter {
    static final String KEY_PREFIX = "faultlens:rule-window:";

    private final StringRedisTemplate redis;
    private final Clock clock;

    @Autowired
    public RuleWindowCounter(StringRedisTemplate redis) {
        this(redis, Clock.systemUTC());
    }

    RuleWindowCounter(StringRedisTemplate redis, Clock clock) {
        this.redis = redis;
        this.clock = clock;
    }

    /**
     * Adds {@code delta} matching entries for the rule now and returns the count over the last
     * {@code windowSec}; empty when Redis is unavailable (the caller must not alarm then).
     */
    public OptionalLong addAndCount(long ruleId, int windowSec, long delta) {
        long minute = clock.millis() / 60_000;
        int buckets = Math.max(1, windowSec / 60);
        try {
            String current = key(ruleId, minute);
            redis.opsForValue().increment(current, delta);
            redis.expire(current, Duration.ofSeconds(windowSec + 60L));
            List<String> keys = new ArrayList<>(buckets);
            for (int i = 0; i < buckets; i++) {
                keys.add(key(ruleId, minute - i));
            }
            List<String> values = redis.opsForValue().multiGet(keys);
            long sum = 0;
            if (values != null) {
                for (String value : values) {
                    if (value != null) {
                        sum += Long.parseLong(value);
                    }
                }
            }
            return OptionalLong.of(sum);
        } catch (RuntimeException ex) {
            log.warn("Rule window counter unavailable (Redis); threshold rule {} skipped", ruleId, ex);
            return OptionalLong.empty();
        }
    }

    static String key(long ruleId, long minute) {
        return KEY_PREFIX + ruleId + ":" + minute;
    }
}
