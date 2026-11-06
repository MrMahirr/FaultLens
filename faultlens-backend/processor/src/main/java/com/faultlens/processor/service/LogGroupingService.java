package com.faultlens.processor.service;

import com.faultlens.common.enums.Severity;
import com.faultlens.processor.event.LogGroupCreatedEvent;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Assigns log entries to groups (same shape of message or stack trace).
 *
 * <p>P08: groups are upserted once per distinct fingerprint per batch with
 * {@code INSERT ... ON CONFLICT DO UPDATE}, in fingerprint order. That replaced a locked
 * select-then-insert per entry, which under parallel consumers deadlocked (rows locked in
 * different orders); after a deadlock the Postgres transaction is aborted, so the rest of the
 * batch failed and went to the DLQ. Counts are incremented atomically in the statement.
 */
@Service
@Transactional
public class LogGroupingService {
    static final String UPSERT = """
            INSERT INTO log_groups (fingerprint, first_message, severity, count, source_id, first_seen_at, last_seen_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (fingerprint) DO UPDATE
               SET count = log_groups.count + EXCLUDED.count,
                   last_seen_at = GREATEST(log_groups.last_seen_at, EXCLUDED.last_seen_at)
            RETURNING id, (xmax = 0) AS inserted
            """;

    private final JdbcTemplate jdbc;
    private final ApplicationEventPublisher events;

    public LogGroupingService(JdbcTemplate jdbc, ApplicationEventPublisher events) {
        this.jdbc = jdbc;
        this.events = events;
    }

    /** One log entry to be grouped. */
    public record GroupCandidate(String message, String stackTrace, Severity severity, Long sourceId,
                                 Instant timestamp) {
    }

    private static final class Aggregate {
        final GroupCandidate first;
        long count;
        Instant firstSeen;
        Instant lastSeen;

        Aggregate(GroupCandidate first) {
            this.first = first;
            this.firstSeen = first.timestamp();
            this.lastSeen = first.timestamp();
        }

        void add(GroupCandidate candidate) {
            count++;
            if (candidate.timestamp().isBefore(firstSeen)) {
                firstSeen = candidate.timestamp();
            }
            if (candidate.timestamp().isAfter(lastSeen)) {
                lastSeen = candidate.timestamp();
            }
        }
    }

    /**
     * Upserts the groups of a batch and returns the group id for each fingerprint. Newly created
     * groups are announced with {@link LogGroupCreatedEvent} (NEW_GROUP alarm rules).
     */
    public Map<String, Long> groupAll(Collection<GroupCandidate> candidates) {
        // Sorted: every transaction locks group rows in the same order -> no deadlocks.
        TreeMap<String, Aggregate> byFingerprint = new TreeMap<>();
        for (GroupCandidate candidate : candidates) {
            byFingerprint.computeIfAbsent(fingerprint(candidate.message(), candidate.stackTrace(), candidate.severity()),
                    key -> new Aggregate(candidate)).add(candidate);
        }
        Map<String, Long> ids = new HashMap<>(byFingerprint.size() * 2);
        byFingerprint.forEach((fingerprint, aggregate) -> {
            GroupCandidate first = aggregate.first;
            jdbc.query(UPSERT, rs -> {
                long id = rs.getLong("id");
                ids.put(fingerprint, id);
                if (rs.getBoolean("inserted")) {
                    events.publishEvent(new LogGroupCreatedEvent(id, first.severity(), first.sourceId(), first.message()));
                }
            }, fingerprint, first.message(), first.severity().name(), aggregate.count, first.sourceId(),
                    Timestamp.from(aggregate.firstSeen), Timestamp.from(aggregate.lastSeen));
        });
        return ids;
    }

    /**
     * Stable fingerprint of a log's shape: its stack trace if it has one, else the severity plus
     * the first 200 characters of the normalized message (numbers, ids, IPs... as placeholders).
     */
    public String fingerprint(String message, String stackTrace, Severity severity) {
        String basis = stackTrace != null && !stackTrace.isBlank()
                ? MessageNormalizer.normalize(stackTrace)
                : severity + ":" + prefix(MessageNormalizer.normalize(message));
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(basis.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private static String prefix(String text) {
        return text.substring(0, Math.min(text.length(), 200));
    }
}
