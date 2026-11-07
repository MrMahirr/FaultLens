package com.faultlens.collector.service;

import com.faultlens.collector.config.CollectorProperties;
import java.net.InetAddress;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

/**
 * Per-source leases in Redis so that, with several instances, exactly one collects each source
 * (P08). Key {@code faultlens:collector:lease:{sourceId}} holds the owner's instance id with a
 * TTL; the owner renews it on every heartbeat, and only the owner can renew or release it.
 *
 * <p>Fails open: without Redis every instance behaves as the owner (single-instance setups keep
 * collecting during a Redis outage; with several instances that outage can duplicate logs).
 */
@Slf4j
@Component
public class SourceLeaseManager {
    static final String KEY_PREFIX = "faultlens:collector:lease:";

    /** Renew only if we still own it. */
    private static final RedisScript<Long> RENEW = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('pexpire', KEYS[1], ARGV[2]) else return 0 end",
            Long.class);
    /** Release only if we still own it. */
    private static final RedisScript<Long> RELEASE = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private final StringRedisTemplate redis;
    private final Duration ttl;
    private final String instanceId;

    @Autowired
    public SourceLeaseManager(StringRedisTemplate redis, CollectorProperties properties) {
        this(redis, properties, defaultInstanceId());
    }

    SourceLeaseManager(StringRedisTemplate redis, CollectorProperties properties, String instanceId) {
        this.redis = redis;
        this.ttl = Duration.ofMillis(properties.getLease().getTtlMs());
        this.instanceId = instanceId;
    }

    static String defaultInstanceId() {
        String host;
        try {
            host = InetAddress.getLocalHost().getHostName();
        } catch (Exception ex) {
            host = "instance";
        }
        return host + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    public String instanceId() {
        return instanceId;
    }

    private static String key(long sourceId) {
        return KEY_PREFIX + sourceId;
    }

    /** True if this instance now holds (or already held) the lease. */
    public boolean tryAcquire(long sourceId) {
        try {
            if (Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key(sourceId), instanceId, ttl))) {
                return true;
            }
            return instanceId.equals(redis.opsForValue().get(key(sourceId))) && renew(sourceId);
        } catch (RuntimeException ex) {
            log.warn("Source leases unavailable (Redis); collecting source {} without a lease", sourceId);
            return true;
        }
    }

    /** Extends our lease; false if another instance owns it now (or it expired). */
    public boolean renew(long sourceId) {
        try {
            Long renewed = redis.execute(RENEW, List.of(key(sourceId)), instanceId, Long.toString(ttl.toMillis()));
            return renewed != null && renewed == 1L;
        } catch (RuntimeException ex) {
            log.warn("Source leases unavailable (Redis); keeping source {}", sourceId);
            return true;
        }
    }

    /**
     * Takes the lease whoever holds it (the source was edited here): the previous owner notices
     * at its next heartbeat and stops.
     */
    public void takeOver(long sourceId) {
        try {
            redis.opsForValue().set(key(sourceId), instanceId, ttl);
        } catch (RuntimeException ex) {
            log.warn("Source leases unavailable (Redis); taking source {} without a lease", sourceId);
        }
    }

    /** Gives up our lease (we stopped collecting). */
    public void release(long sourceId) {
        try {
            redis.execute(RELEASE, List.of(key(sourceId)), instanceId);
        } catch (RuntimeException ex) {
            log.debug("Could not release lease of source {}", sourceId);
        }
    }

    /** Removes the lease whoever holds it (the source was disabled or deleted). */
    public void revoke(long sourceId) {
        try {
            redis.delete(key(sourceId));
        } catch (RuntimeException ex) {
            log.debug("Could not revoke lease of source {}", sourceId);
        }
    }

    /** The instance currently collecting the source; empty if nobody (or Redis is down). */
    public Optional<String> ownerOf(long sourceId) {
        try {
            return Optional.ofNullable(redis.opsForValue().get(key(sourceId)));
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }
}
