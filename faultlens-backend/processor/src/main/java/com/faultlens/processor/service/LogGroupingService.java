package com.faultlens.processor.service;

import com.faultlens.common.enums.Severity;
import com.faultlens.common.enums.GroupStatus;
import com.faultlens.processor.event.LogGroupCreatedEvent;
import com.faultlens.processor.event.LogGroupRegressedEvent;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    /*
     * Lifecycle (P10 / 2.1, ADR 0004), decided in the same statement as the count so concurrent
     * batches cannot both see "RESOLVED" and double-count a regression:
     *  - RESOLVED and the batch's newest entry is after resolved_at -> UNRESOLVED, regression.
     *    (Late entries older than the resolution only add to the count.)
     *  - IGNORED with an expired ignore_until -> UNRESOLVED, no regression.
     * In SET, log_groups.* is the row before the update; RETURNING old.* / new.* (PostgreSQL 18)
     * tells which transition happened without a second query.
     */
    private static final String REGRESSES =
            "(log_groups.status = 'RESOLVED' AND EXCLUDED.last_seen_at > log_groups.resolved_at)";
    private static final String UNIGNORES =
            "(log_groups.status = 'IGNORED' AND log_groups.ignore_until IS NOT NULL AND log_groups.ignore_until <= NOW())";
    private static final String REOPENS = "(" + REGRESSES + " OR " + UNIGNORES + ")";

    static final String UPSERT = """
            INSERT INTO log_groups (fingerprint, first_message, severity, count, source_id, first_seen_at, last_seen_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (fingerprint) DO UPDATE
               SET count = log_groups.count + EXCLUDED.count,
                   last_seen_at = GREATEST(log_groups.last_seen_at, EXCLUDED.last_seen_at),
                   status = CASE WHEN %1$s THEN 'UNRESOLVED' ELSE log_groups.status END,
                   resolved_at = CASE WHEN %1$s THEN NULL ELSE log_groups.resolved_at END,
                   resolved_by = CASE WHEN %1$s THEN NULL ELSE log_groups.resolved_by END,
                   ignore_until = CASE WHEN %1$s THEN NULL ELSE log_groups.ignore_until END,
                   regressed_at = CASE WHEN %2$s THEN NOW() ELSE log_groups.regressed_at END,
                   regression_count = log_groups.regression_count + CASE WHEN %2$s THEN 1 ELSE 0 END
            RETURNING new.id, (xmax = 0) AS inserted, old.status AS old_status, new.status AS new_status,
                      new.regression_count
            """.formatted(REOPENS, REGRESSES);

    static final String INSERT_EVENT = """
            INSERT INTO log_group_events (group_id, type, payload) VALUES (?, ?, ?::jsonb)
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
     * Group ids per fingerprint, and the groups that are muted (IGNORED) after this batch: their
     * entries are stored but raise no alarms.
     */
    public record GroupingResult(Map<String, Long> groupIds, Set<Long> mutedGroupIds) {
    }

    /**
     * Upserts the groups of a batch and returns the group id for each fingerprint. Newly created
     * groups are announced with {@link LogGroupCreatedEvent} (NEW_GROUP alarm rules), resolved
     * groups seen again with {@link LogGroupRegressedEvent} (REGRESSION rules); both, and ignore
     * expiries, are written to the group's timeline ({@code log_group_events}).
     */
    public GroupingResult groupAll(Collection<GroupCandidate> candidates) {
        // Sorted: every transaction locks group rows in the same order -> no deadlocks.
        TreeMap<String, Aggregate> byFingerprint = new TreeMap<>();
        for (GroupCandidate candidate : candidates) {
            byFingerprint.computeIfAbsent(fingerprint(candidate.message(), candidate.stackTrace(), candidate.severity()),
                    key -> new Aggregate(candidate)).add(candidate);
        }
        Map<String, Long> ids = new HashMap<>(byFingerprint.size() * 2);
        Set<Long> muted = new HashSet<>();
        List<Object[]> timeline = new ArrayList<>();
        byFingerprint.forEach((fingerprint, aggregate) -> {
            GroupCandidate first = aggregate.first;
            jdbc.query(UPSERT, rs -> {
                long id = rs.getLong("id");
                ids.put(fingerprint, id);
                GroupStatus newStatus = GroupStatus.valueOf(rs.getString("new_status"));
                if (newStatus == GroupStatus.IGNORED) {
                    muted.add(id);
                }
                if (rs.getBoolean("inserted")) {
                    timeline.add(new Object[] {id, "CREATED", null});
                    events.publishEvent(new LogGroupCreatedEvent(id, first.severity(), first.sourceId(), first.message()));
                    return;
                }
                String oldStatus = rs.getString("old_status");
                if (newStatus != GroupStatus.UNRESOLVED || oldStatus == null) {
                    return;
                }
                if (GroupStatus.RESOLVED.name().equals(oldStatus)) {
                    int regressions = rs.getInt("regression_count");
                    timeline.add(new Object[] {id, "REGRESSED", "{\"regression_count\": " + regressions + "}"});
                    events.publishEvent(new LogGroupRegressedEvent(id, first.severity(), first.sourceId(),
                            first.message(), regressions));
                } else if (GroupStatus.IGNORED.name().equals(oldStatus)) {
                    timeline.add(new Object[] {id, "UNIGNORED", null});
                }
            }, fingerprint, first.message(), first.severity().name(), aggregate.count, first.sourceId(),
                    Timestamp.from(aggregate.firstSeen), Timestamp.from(aggregate.lastSeen));
        });
        if (!timeline.isEmpty()) {
            jdbc.batchUpdate(INSERT_EVENT, timeline);
        }
        return new GroupingResult(ids, muted);
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
