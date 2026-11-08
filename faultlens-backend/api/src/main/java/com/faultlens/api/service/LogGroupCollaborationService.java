package com.faultlens.api.service;

import com.faultlens.api.dto.LogGroupDtos.AssignableUser;
import com.faultlens.api.dto.LogGroupDtos.LogGroupCommentDto;
import com.faultlens.api.mapper.ApiDtoMapper;
import com.faultlens.api.model.LogGroupEventType;
import com.faultlens.api.model.UserAccount;
import com.faultlens.api.repository.UserAccountRepository;
import com.faultlens.common.dto.LogGroupDto;
import com.faultlens.common.exception.ConflictException;
import com.faultlens.common.exception.FaultLensException;
import com.faultlens.common.exception.ResourceNotFoundException;
import com.faultlens.processor.model.LogGroup;
import com.faultlens.processor.repository.LogGroupRepository;
import io.micrometer.core.instrument.Metrics;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Working on a log group together (P10 / 2.2, ADR 0004): assignment, comments, merges. Every
 * change is written to the group's timeline ({@code log_group_events}).
 *
 * <p>Writes go through SQL, never through the managed {@link LogGroup}: Hibernate would flush
 * every column at commit and overwrite the count the processor may have written meanwhile.
 */
@Service
public class LogGroupCollaborationService {
    private static final String INSERT_EVENT = """
            INSERT INTO log_group_events (group_id, type, actor, payload, created_at)
            VALUES (?, ?, ?, jsonb_strip_nulls(jsonb_build_object('assignee', ?::text, 'related_group_id', ?::bigint)), ?)
            """;
    private static final String COMMENT_COLUMNS = "id, group_id, author, body, created_at, edited_at";
    private static final RowMapper<LogGroupCommentDto> COMMENT = (rs, row) -> new LogGroupCommentDto(
            rs.getLong("id"), rs.getLong("group_id"), rs.getString("author"), rs.getString("body"),
            rs.getTimestamp("created_at").toInstant(),
            rs.getTimestamp("edited_at") == null ? null : rs.getTimestamp("edited_at").toInstant());

    private final LogGroupRepository groups;
    private final UserAccountRepository users;
    private final JdbcTemplate jdbc;
    private final ApiDtoMapper mapper;
    private final InAppNotificationService notifications;
    private final Clock clock;

    @Autowired
    public LogGroupCollaborationService(LogGroupRepository groups, UserAccountRepository users, JdbcTemplate jdbc,
                                        ApiDtoMapper mapper, InAppNotificationService notifications) {
        this(groups, users, jdbc, mapper, notifications, Clock.systemUTC());
    }

    LogGroupCollaborationService(LogGroupRepository groups, UserAccountRepository users, JdbcTemplate jdbc,
                                 ApiDtoMapper mapper, InAppNotificationService notifications, Clock clock) {
        this.groups = groups;
        this.users = users;
        this.jdbc = jdbc;
        this.mapper = mapper;
        this.notifications = notifications;
        this.clock = clock;
    }

    /* ── Assignment ─────────────────────────────────────────── */

    /** Everyone a group can be assigned to, by name. */
    @Transactional(readOnly = true)
    public List<AssignableUser> assignableUsers() {
        return users.findAllByOrderByUsernameAsc().stream().map(user -> new AssignableUser(user.getUsername())).toList();
    }

    /**
     * Assigns the group to {@code assignee} (an existing user; 400 UNKNOWN_USER otherwise) or,
     * with null/blank, unassigns it. Assigning to the current assignee again is 409. The new
     * assignee is notified in-app after commit, unless they assigned themselves.
     */
    @Transactional
    public LogGroupDto assign(Long id, String assignee, String actor) {
        LogGroup group = find(id);
        String requested = assignee == null || assignee.isBlank() ? null : assignee.trim();
        String target = requested == null ? null : users.findByUsername(requested)
                .map(UserAccount::getUsername)
                .orElseThrow(() -> new FaultLensException("No such user: " + requested, "UNKNOWN_USER"));
        if (java.util.Objects.equals(group.getAssignee(), target)) {
            throw new ConflictException(target == null ? "Log group is not assigned"
                    : "Log group is already assigned to " + target, "GROUP_ASSIGNEE_UNCHANGED");
        }
        Instant now = now();
        jdbc.update("UPDATE log_groups SET assignee = ?, assigned_at = ? WHERE id = ?",
                target, target == null ? null : Timestamp.from(now), id);
        event(id, target == null ? LogGroupEventType.UNASSIGNED : LogGroupEventType.ASSIGNED, actor, target, null, now);
        Metrics.counter("faultlens.log_groups.assignments", "assigned", String.valueOf(target != null)).increment();

        if (target != null && !target.equals(actor)) {
            String notified = target;
            afterCommit(() -> notifications.notifyAssigned(notified, id, group.getFirstMessage(), actor));
        }
        LogGroupDto dto = mapper.toLogGroupDto(group);
        dto.setAssignee(target);
        dto.setAssignedAt(target == null ? null : now);
        return dto;
    }

    /* ── Comments ───────────────────────────────────────────── */

    /** The group's comments, oldest first (a conversation). */
    @Transactional(readOnly = true)
    public List<LogGroupCommentDto> comments(Long groupId) {
        find(groupId);
        return jdbc.query("SELECT " + COMMENT_COLUMNS + " FROM log_group_comments WHERE group_id = ? "
                + "ORDER BY created_at, id", COMMENT, groupId);
    }

    @Transactional
    public LogGroupCommentDto addComment(Long groupId, String body, String author) {
        find(groupId);
        Instant now = now();
        LogGroupCommentDto comment = jdbc.queryForObject("""
                INSERT INTO log_group_comments (group_id, author, body, created_at) VALUES (?, ?, ?, ?)
                RETURNING %s
                """.formatted(COMMENT_COLUMNS), COMMENT, groupId, author, body.strip(), Timestamp.from(now));
        event(groupId, LogGroupEventType.COMMENTED, author, null, null, now);
        return comment;
    }

    /** Only the author edits a comment (403 otherwise). */
    @Transactional
    public LogGroupCommentDto editComment(Long groupId, Long commentId, String body, String actor) {
        LogGroupCommentDto existing = findComment(groupId, commentId);
        if (!existing.author().equals(actor)) {
            throw new AccessDeniedException("Only the author can edit a comment");
        }
        return jdbc.queryForObject("""
                UPDATE log_group_comments SET body = ?, edited_at = ? WHERE id = ? RETURNING %s
                """.formatted(COMMENT_COLUMNS), COMMENT, body.strip(), Timestamp.from(now()), commentId);
    }

    /** The author or an admin deletes a comment (403 otherwise). */
    @Transactional
    public void deleteComment(Long groupId, Long commentId, String actor, boolean admin) {
        LogGroupCommentDto existing = findComment(groupId, commentId);
        if (!admin && !existing.author().equals(actor)) {
            throw new AccessDeniedException("Only the author or an admin can delete a comment");
        }
        jdbc.update("DELETE FROM log_group_comments WHERE id = ?", commentId);
    }

    /* ── Merge ──────────────────────────────────────────────── */

    /**
     * Merges {@code sourceId} into {@code targetId}: new entries of the source go to the target
     * (processor, LogGroupingService); past entries stay where they are. Groups already merged
     * into the source are re-pointed to the target, so merges never chain. Undo with
     * {@link #unmerge}.
     */
    @Transactional
    public LogGroupDto merge(Long sourceId, Long targetId, String actor) {
        if (sourceId.equals(targetId)) {
            throw new FaultLensException("A group cannot be merged into itself", "INVALID_MERGE");
        }
        // Locked in fingerprint order, like the processor's upsert: no deadlock with log batches.
        Map<Long, Long> mergedInto = new HashMap<>();
        jdbc.query("SELECT id, merged_into_id FROM log_groups WHERE id IN (?, ?) ORDER BY fingerprint FOR UPDATE",
                rs -> {
                    mergedInto.put(rs.getLong("id"), (Long) rs.getObject("merged_into_id"));
                }, sourceId, targetId);
        if (!mergedInto.containsKey(sourceId)) {
            throw notFound(sourceId);
        }
        if (!mergedInto.containsKey(targetId)) {
            throw notFound(targetId);
        }
        if (mergedInto.get(sourceId) != null) {
            throw new ConflictException("Log group " + sourceId + " is already merged into " + mergedInto.get(sourceId),
                    "GROUP_ALREADY_MERGED");
        }
        if (mergedInto.get(targetId) != null) {
            throw new FaultLensException("Log group " + targetId + " is merged into " + mergedInto.get(targetId)
                    + "; merge into that group instead", "INVALID_MERGE");
        }
        Instant now = now();
        Timestamp at = Timestamp.from(now);
        List<Long> repointed = jdbc.queryForList(
                "UPDATE log_groups SET merged_into_id = ?, merged_at = ? WHERE merged_into_id = ? RETURNING id",
                Long.class, targetId, at, sourceId);
        jdbc.update("UPDATE log_groups SET merged_into_id = ?, merged_at = ? WHERE id = ?", targetId, at, sourceId);

        event(sourceId, LogGroupEventType.MERGED, actor, null, targetId, now);
        event(targetId, LogGroupEventType.MERGED_FROM, actor, null, sourceId, now);
        for (Long child : repointed) {
            event(child, LogGroupEventType.MERGED, actor, null, targetId, now);
            event(targetId, LogGroupEventType.MERGED_FROM, actor, null, child, now);
        }
        Metrics.counter("faultlens.log_groups.merges", "action", "merge").increment();
        return withMerge(sourceId, targetId, now);
    }

    /** Undoes a merge: the group gets its own new entries again (entries routed meanwhile stay). */
    @Transactional
    public LogGroupDto unmerge(Long id, String actor) {
        List<Long> rows = jdbc.query("SELECT merged_into_id FROM log_groups WHERE id = ? FOR UPDATE",
                (rs, row) -> (Long) rs.getObject("merged_into_id"), id);
        if (rows.isEmpty()) {
            throw notFound(id);
        }
        Long target = rows.getFirst();
        if (target == null) {
            throw new ConflictException("Log group " + id + " is not merged", "GROUP_NOT_MERGED");
        }
        Instant now = now();
        jdbc.update("UPDATE log_groups SET merged_into_id = NULL, merged_at = NULL WHERE id = ?", id);
        event(id, LogGroupEventType.UNMERGED, actor, null, target, now);
        event(target, LogGroupEventType.UNMERGED, actor, null, id, now);
        Metrics.counter("faultlens.log_groups.merges", "action", "unmerge").increment();
        return withMerge(id, null, null);
    }

    /* ── Helpers ────────────────────────────────────────────── */

    private LogGroupDto withMerge(Long id, Long target, Instant at) {
        LogGroupDto dto = mapper.toLogGroupDto(find(id));
        dto.setMergedIntoId(target);
        dto.setMergedAt(at);
        return dto;
    }

    private void event(Long groupId, LogGroupEventType type, String actor, String assignee, Long related, Instant at) {
        jdbc.update(INSERT_EVENT, groupId, type.name(), actor, assignee, related, Timestamp.from(at));
    }

    private LogGroupCommentDto findComment(Long groupId, Long commentId) {
        List<LogGroupCommentDto> found = jdbc.query("SELECT " + COMMENT_COLUMNS
                + " FROM log_group_comments WHERE id = ? AND group_id = ?", COMMENT, commentId, groupId);
        if (found.isEmpty()) {
            throw new ResourceNotFoundException("Comment not found: " + commentId, "COMMENT_NOT_FOUND");
        }
        return found.getFirst();
    }

    private LogGroup find(Long id) {
        return groups.findById(id).orElseThrow(() -> notFound(id));
    }

    private static ResourceNotFoundException notFound(Long id) {
        return new ResourceNotFoundException("Log group not found: " + id, "GROUP_NOT_FOUND");
    }

    private Instant now() {
        // timestamptz keeps microseconds; truncating keeps responses equal to later reads.
        return clock.instant().truncatedTo(ChronoUnit.MICROS);
    }

    private static void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}
