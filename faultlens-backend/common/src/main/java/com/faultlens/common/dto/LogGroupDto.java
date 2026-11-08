package com.faultlens.common.dto;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;
import com.faultlens.common.enums.GroupStatus;
import com.faultlens.common.enums.Severity;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class LogGroupDto {
    private Long id;
    private String fingerprint;
    private String firstMessage;
    private Severity severity;
    private Long count;
    private Instant firstSeenAt;
    private Instant lastSeenAt;
    private Long sourceId;
    /** Lifecycle status (P10 / 2.1). */
    private GroupStatus status;
    private Instant resolvedAt;
    /** Username of whoever resolved or ignored the group. */
    private String resolvedBy;
    /** IGNORED until this time; null = until reopened. */
    private Instant ignoreUntil;
    /** Last time a RESOLVED group was seen again. */
    private Instant regressedAt;
    private Integer regressionCount;
    /** Username of the person working on it (P10 / 2.2). */
    private String assignee;
    private Instant assignedAt;
    /** Merged into this group: its new entries go there. */
    private Long mergedIntoId;
    private Instant mergedAt;
}
