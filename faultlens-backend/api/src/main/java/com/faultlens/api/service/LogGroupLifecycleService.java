package com.faultlens.api.service;

import com.faultlens.api.dto.LogGroupDtos.GroupStatusUpdateRequest;
import com.faultlens.api.dto.LogGroupDtos.LogGroupEventDto;
import com.faultlens.api.mapper.ApiDtoMapper;
import com.faultlens.api.model.LogGroupEventType;
import com.faultlens.api.service.LogGroupTimeline.Details;
import com.faultlens.common.dto.LogGroupDto;
import com.faultlens.common.enums.GroupStatus;
import com.faultlens.common.exception.ConflictException;
import com.faultlens.processor.model.LogGroup;
import io.micrometer.core.instrument.Metrics;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
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
    private static final String UPDATE = """
            UPDATE log_groups
               SET status = ?, resolved_at = ?, resolved_by = ?, ignore_until = ?
             WHERE id = ? AND status = ?
            """;

    private final LogGroupTimeline timeline;
    private final JdbcTemplate jdbc;
    private final ApiDtoMapper mapper;

    public LogGroupLifecycleService(LogGroupTimeline timeline, JdbcTemplate jdbc, ApiDtoMapper mapper) {
        this.timeline = timeline;
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    /** One group (404 GROUP_NOT_FOUND). */
    @Transactional(readOnly = true)
    public LogGroupDto get(Long id) {
        return mapper.toLogGroupDto(timeline.find(id));
    }

    /**
     * Resolves, ignores or reopens a group and records it on the timeline.
     *
     * @param actor username of the caller
     */
    @Transactional
    public LogGroupDto updateStatus(Long id, GroupStatusUpdateRequest request, String actor) {
        LogGroup group = timeline.find(id);
        GroupStatus from = group.getStatus();
        GroupStatus to = request.status();
        Instant now = timeline.now();
        Instant ignoreUntil = to == GroupStatus.IGNORED && request.ignoreUntil() != null
                ? request.ignoreUntil().truncatedTo(ChronoUnit.MICROS) : null;
        LogGroupEventType type = GroupStateMachine.transition(from, to, request.ignoreUntil(),
                group.getIgnoreUntil(), now);

        boolean open = to == GroupStatus.UNRESOLVED;
        int updated = jdbc.update(UPDATE, to.name(), open ? null : Timestamp.from(now), open ? null : actor,
                ignoreUntil == null ? null : Timestamp.from(ignoreUntil), id, from.name());
        if (updated == 0) {
            throw new ConflictException("Log group " + id + " changed meanwhile; reload and try again",
                    "GROUP_STATUS_CONFLICT");
        }
        timeline.write(id, type, actor, Details.fromStatus(from, ignoreUntil), now);
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

    /** The newest 200 timeline entries of a group, newest first. */
    @Transactional(readOnly = true)
    public List<LogGroupEventDto> timeline(Long id) {
        return timeline.read(id);
    }
}
