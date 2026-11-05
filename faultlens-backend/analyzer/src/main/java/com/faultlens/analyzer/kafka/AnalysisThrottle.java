package com.faultlens.analyzer.kafka;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * At most one automatic analysis per log group per hour, across all instances.
 *
 * <p>The database check in {@link LogErrorConsumer} ("analysed in the last hour?") is a
 * check-then-act: two consumers handling errors of the same group at the same time both pass it
 * and both analyse (and raise two alarms). {@code SET key NX EX} is atomic, so only one wins.
 * When Redis is unavailable this allows the analysis and the database check alone applies.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnalysisThrottle {
    static final Duration WINDOW = Duration.ofHours(1);
    static final String KEY_PREFIX = "faultlens:analysis:group:";

    private final StringRedisTemplate redis;

    /**
     * True if this caller may analyse the group now (and claims the window).
     */
    public boolean tryAcquire(Long groupId) {
        if (groupId == null) {
            return true;
        }
        try {
            return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(KEY_PREFIX + groupId, "1", WINDOW));
        } catch (RuntimeException ex) {
            log.warn("Analysis throttle unavailable (Redis); relying on the database check", ex);
            return true;
        }
    }
}
