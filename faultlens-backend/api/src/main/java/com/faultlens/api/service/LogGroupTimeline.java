package com.faultlens.api.service;

import com.faultlens.api.dto.LogGroupDtos.LogGroupEventDto;
import com.faultlens.api.model.LogGroupEventType;
import com.faultlens.common.enums.GroupStatus;
import com.faultlens.common.exception.ResourceNotFoundException;
import com.faultlens.processor.model.LogGroup;
import com.faultlens.processor.repository.LogGroupRepository;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * What the API services share about a log group (P10 / 2.1–2.2, ADR 0004): finding it, the
 * clock, and writing/reading its timeline ({@code log_group_events}). The payload keys written
 * here and read back by {@link #read} are defined once, in this class. (The processor's
 * LogGroupingService writes CREATED / REGRESSED / UNIGNORED with the same keys.)
 */
@Component
public class LogGroupTimeline {
    static final int LIMIT = 200;

    private static final String INSERT = """
            INSERT INTO log_group_events (group_id, type, actor, payload, created_at)
            VALUES (?, ?, ?, jsonb_strip_nulls(jsonb_build_object(
                    'from_status', ?::text, 'ignore_until', ?::timestamptz,
                    'assignee', ?::text, 'related_group_id', ?::bigint)), ?)
            """;
    private static final String READ = """
            SELECT id, type, actor, payload->>'from_status' AS from_status,
                   (payload->>'ignore_until')::timestamptz AS ignore_until,
                   (payload->>'regression_count')::int AS regression_count, payload->>'assignee' AS assignee,
                   (payload->>'related_group_id')::bigint AS related_group_id, created_at
              FROM log_group_events
             WHERE group_id = ?
             ORDER BY created_at DESC, id DESC
             LIMIT ?
            """;

    /** Optional payload of a timeline entry; null fields are not stored. */
    public record Details(GroupStatus fromStatus, Instant ignoreUntil, String assignee, Long relatedGroupId) {
        public static final Details NONE = new Details(null, null, null, null);

        public static Details fromStatus(GroupStatus from, Instant ignoreUntil) {
            return new Details(from, ignoreUntil, null, null);
        }

        public static Details assignee(String assignee) {
            return new Details(null, null, assignee, null);
        }

        public static Details related(Long groupId) {
            return new Details(null, null, null, groupId);
        }
    }

    private final LogGroupRepository groups;
    private final JdbcTemplate jdbc;
    private final Clock clock;

    @Autowired
    public LogGroupTimeline(LogGroupRepository groups, JdbcTemplate jdbc) {
        this(groups, jdbc, Clock.systemUTC());
    }

    LogGroupTimeline(LogGroupRepository groups, JdbcTemplate jdbc, Clock clock) {
        this.groups = groups;
        this.jdbc = jdbc;
        this.clock = clock;
    }

    /** The group, or 404 GROUP_NOT_FOUND. */
    public LogGroup find(Long id) {
        return groups.findById(id).orElseThrow(() -> notFound(id));
    }

    public static ResourceNotFoundException notFound(Long id) {
        return new ResourceNotFoundException("Log group not found: " + id, "GROUP_NOT_FOUND");
    }

    /** Now, truncated to microseconds like timestamptz, so responses equal later reads. */
    public Instant now() {
        return clock.instant().truncatedTo(ChronoUnit.MICROS);
    }

    public void write(Long groupId, LogGroupEventType type, String actor, Details details, Instant at) {
        jdbc.update(INSERT, groupId, type.name(), actor,
                details.fromStatus() == null ? null : details.fromStatus().name(),
                details.ignoreUntil() == null ? null : Timestamp.from(details.ignoreUntil()),
                details.assignee(), details.relatedGroupId(), Timestamp.from(at));
    }

    /** The newest {@value #LIMIT} entries, newest first (404 if the group does not exist). */
    public List<LogGroupEventDto> read(Long groupId) {
        find(groupId);
        return jdbc.query(READ, (rs, row) -> {
            Timestamp ignoreUntil = rs.getTimestamp("ignore_until");
            String fromStatus = rs.getString("from_status");
            return new LogGroupEventDto(
                    rs.getLong("id"),
                    LogGroupEventType.valueOf(rs.getString("type")),
                    rs.getString("actor"),
                    fromStatus == null ? null : GroupStatus.valueOf(fromStatus),
                    ignoreUntil == null ? null : ignoreUntil.toInstant(),
                    (Integer) rs.getObject("regression_count"),
                    rs.getString("assignee"),
                    (Long) rs.getObject("related_group_id"),
                    rs.getTimestamp("created_at").toInstant());
        }, groupId, LIMIT);
    }
}
