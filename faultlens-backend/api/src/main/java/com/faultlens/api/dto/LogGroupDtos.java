package com.faultlens.api.dto;

import com.faultlens.api.model.LogGroupEventType;
import com.faultlens.common.enums.GroupStatus;
import jakarta.validation.constraints.NotNull;
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
            Instant createdAt) {
    }
}
