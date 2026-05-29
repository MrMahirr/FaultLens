package com.faultlens.processor.service;

import com.faultlens.common.enums.Severity;
import com.faultlens.processor.model.LogGroup;
import com.faultlens.processor.repository.LogGroupRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class LogGroupingService {
    private final LogGroupRepository repository;
    private final LogGroupingService self;

    public LogGroupingService(LogGroupRepository repository, @Lazy LogGroupingService self) {
        this.repository = repository;
        this.self = self;
    }

    /**
     * Finds or creates the group for a parsed log.
     */
    public LogGroup group(String message, String stackTrace, Severity severity, Long sourceId, Instant timestamp) {
        String fingerprint = fingerprint(message, stackTrace, severity);
        return repository.findWithLockByFingerprint(fingerprint)
                .map(group -> updateGroup(group, timestamp))
                .orElseGet(() -> self.createGroupSafely(fingerprint, message, severity, sourceId, timestamp));
    }

    /**
     * Creates a group safely, catching concurrent inserts.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LogGroup createGroupSafely(String fingerprint, String message, Severity severity, Long sourceId, Instant timestamp) {
        try {
            return repository.saveAndFlush(LogGroup.builder()
                    .fingerprint(fingerprint)
                    .firstMessage(message)
                    .severity(severity)
                    .count(1L)
                    .sourceId(sourceId)
                    .firstSeenAt(timestamp)
                    .lastSeenAt(timestamp)
                    .build());
        } catch (DataIntegrityViolationException e) {
            // Created concurrently by another thread, fetch and update it
            return repository.findWithLockByFingerprint(fingerprint)
                    .map(group -> updateGroup(group, timestamp))
                    .orElseThrow(() -> new IllegalStateException("Failed to fetch concurrently created group"));
        }
    }

    /**
     * Creates a stable fingerprint from stack trace or message prefix.
     */
    public String fingerprint(String message, String stackTrace, Severity severity) {
        String basis = stackTrace != null && !stackTrace.isBlank()
                ? stackTrace.trim()
                : severity + ":" + (message == null ? "" : message.substring(0, Math.min(message.length(), 200)));
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(basis.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private LogGroup updateGroup(LogGroup group, Instant timestamp) {
        group.setCount(group.getCount() == null ? 1L : group.getCount() + 1);
        group.setLastSeenAt(timestamp == null ? Instant.now() : timestamp);
        return repository.save(group);
    }
}
