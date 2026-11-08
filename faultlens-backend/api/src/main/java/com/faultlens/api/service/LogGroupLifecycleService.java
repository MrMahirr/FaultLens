package com.faultlens.api.service;

import com.faultlens.api.dto.LogGroupDtos.GroupStatusUpdateRequest;
import com.faultlens.api.dto.LogGroupDtos.LogGroupEventDto;
import com.faultlens.api.mapper.ApiDtoMapper;
import com.faultlens.api.model.LogGroupEventType;
import com.faultlens.common.dto.LogGroupDto;
import com.faultlens.common.enums.GroupStatus;
import com.faultlens.common.exception.ConflictException;
import com.faultlens.common.exception.ResourceNotFoundException;
import com.faultlens.processor.model.LogGroup;
import com.faultlens.processor.repository.LogGroupRepository;
import io.micrometer.core.instrument.Metrics;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manual log group status changes and the group timeline (P10 / 2.1, ADR 0004).
 *
 * <p>The update is guarded by the status the change was checked against
 * ({@code WHERE status = ?}): if the processor reopened the group in between (regression), the
 * user's change is refused with 409 instead of silently overwriting the regression.
 */
@Service
public class LogGroupLifecycleService {
    static final int TIMELINE_LIMIT = 200;

    private static final String UPDATE = """
            UPDATE log_groups
               SET status = ?, resolved_at = ?, resolved_by = ?, ignore_until = ?
             WHERE id = ? AND status = ?
            """;
    private static final String INSERT_EVENT = """
            INSERT INTO log_group_events (group_id, type, actor, payload, created_at)
            VALUES (?, ?, ?, jsonb_strip_nulls(jsonb_build_object('from_status', ?::text, 'ignore_until', ?::timestamptz)), ?)
            """;
    private static final String TIMELINE = """
            SELECT id, type, actor, payload->>'from_status' AS from_status,
                   (payload->>'ignore_until')::timestamptz AS ignore_until,
                   (payload->>'regression_count')::int AS regression_count, created_at
              FROM log_group_events
             WHERE group_id = ?
             ORDER BY created_at DESC, id DESC
             LIMIT ?
            """;

    private final LogGroupRepository groups;
    private final JdbcTemplate jdbc;
    private final ApiDtoMapper mapper;
    private final Clock clock;

    @Autowired
    public LogGroupLifecycleService(LogGroupRepository groups, JdbcTemplate jdbc, ApiDtoMapper mapper) {
        this(groups, jdbc, mapper, Clock.systemUTC());
    }

    LogGroupLifecycleService(LogGroupRepository groups, JdbcTemplate jdbc, ApiDtoMapper mapper, Clock clock) {
        this.groups = groups;
        this.jdbc = jdbc;
        this.mapper = mapper;
        this.clock = clock;
    }

    /** One group (404 GROUP_NOT_FOUND). */
    @Transactional(readOnly = true)
    public LogGroupDto get(Long id) {
        return mapper.toLogGroupDto(find(id));
    }

    /**
     * Resolves, ignores or reopens a group and records it on the timeline.
     *
     * @param actor username of the caller
     */
    @Transactional
    public LogGroupDto updateStatus(Long id, GroupStatusUpdateRequest request, String actor) {
        LogGroup group = find(id);
        GroupStatus from = group.getStatus();
        GroupStatus to = request.status();
        // timestamptz keeps microseconds; truncating keeps the response equal to later reads.
        Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
        LogGroupEventType type = GroupStateMachine.transition(from, to, request.ignoreUntil(), now);

        boolean open = to == GroupStatus.UNRESOLVED;
        Instant ignoreUntil = to == GroupStatus.IGNORED && request.ignoreUntil() != null
                ? request.ignoreUntil().truncatedTo(ChronoUnit.MICROS) : null;
        int updated = jdbc.update(UPDATE, to.name(), open ? null : Timestamp.from(now), open ? null : actor,
                ignoreUntil == null ? null : Timestamp.from(ignoreUntil), id, from.name());
        if (updated == 0) {
            throw new ConflictException("Log group " + id + " changed meanwhile; reload and try again",
                    "GROUP_STATUS_CONFLICT");
        }
        jdbc.update(INSERT_EVENT, id, type.name(), actor, from.name(),
                ignoreUntil == null ? null : Timestamp.from(ignoreUntil), Timestamp.from(now));
        Metrics.counter("faultlens.log_groups.status_changes", "to", to.name()).increment();

        // Not on the managed entity: Hibernate would flush every column at commit and overwrite
        // the count / last_seen_at the processor may have written since we read the group.
        LogGroupDto dto = mapper.toLogGroupDto(group);
        dto.setStatus(to);
        dto.setResolvedAt(open ? null : now);
        dto.setResolvedBy(open ? null : actor);
        dto.setIgnoreUntil(ignoreUntil);
        return dto;
    }

    /** The newest {@value #TIMELINE_LIMIT} timeline entries of a group, newest first. */
    @Transactional(readOnly = true)
    public List<LogGroupEventDto> timeline(Long id) {
        find(id);
        return jdbc.query(TIMELINE, (rs, row) -> {
            Timestamp ignoreUntil = rs.getTimestamp("ignore_until");
            String fromStatus = rs.getString("from_status");
            return new LogGroupEventDto(
                    rs.getLong("id"),
                    LogGroupEventType.valueOf(rs.getString("type")),
                    rs.getString("actor"),
                    fromStatus == null ? null : GroupStatus.valueOf(fromStatus),
                    ignoreUntil == null ? null : ignoreUntil.toInstant(),
                    (Integer) rs.getObject("regression_count"),
                    rs.getTimestamp("created_at").toInstant());
        }, id, TIMELINE_LIMIT);
    }

    private LogGroup find(Long id) {
        return groups.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Log group not found: " + id, "GROUP_NOT_FOUND"));
    }
}
