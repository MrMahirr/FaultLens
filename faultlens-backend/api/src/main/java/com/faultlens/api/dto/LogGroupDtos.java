package com.faultlens.api.dto;

import com.faultlens.api.model.LogGroupEventType;
import com.faultlens.common.enums.GroupStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/** Log group lifecycle payloads (P10 / 2.1, ADR 0004). */
public final class LogGroupDtos {
    private LogGroupDtos() {
    }

    /**
     * {@code PATCH /api/v1/logs/groups/{id}}. {@code ignore_until} only with {@code IGNORED};
     * omitted = ignored until reopened by hand.
     */
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record GroupStatusUpdateRequest(@NotNull GroupStatus status, Instant ignoreUntil) {
    }

    /**
     * One entry of a group's timeline. {@code actor} is a username, null for system transitions
     * (regression, ignore expiry); the other optional fields depend on {@code type}.
     */
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record LogGroupEventDto(
            Long id,
            LogGroupEventType type,
            String actor,
            /** Status before a manual change (RESOLVED, IGNORED, REOPENED). */
            GroupStatus fromStatus,
            /** IGNORED with a time limit. */
            Instant ignoreUntil,
            /** REGRESSED: the group's regression count after this one. */
            Integer regressionCount,
            /** ASSIGNED: who it was assigned to. */
            String assignee,
            /** MERGED, MERGED_FROM, UNMERGED: the other group. */
            Long relatedGroupId,
            Instant createdAt) {
    }

    /** {@code PUT /api/v1/logs/groups/{id}/assignee}; null or blank unassigns. */
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record AssignRequest(@Size(max = 100) String assignee) {
    }

    /** {@code POST /api/v1/logs/groups/{id}/merge}: merge this group into {@code target_id}. */
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record MergeRequest(@NotNull Long targetId) {
    }

    /** Plain text; shown as text, never rendered as HTML. */
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record CommentRequest(@NotBlank @Size(max = 10_000) String body) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record LogGroupCommentDto(Long id, Long groupId, String author, String body, Instant createdAt,
                                     Instant editedAt) {
    }

    /** A user a group can be assigned to (editors may not list users, so only the name). */
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record AssignableUser(String username) {
    }
}
