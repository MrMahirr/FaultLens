package com.faultlens.processor.model;

import com.faultlens.common.enums.GroupStatus;
import com.faultlens.common.enums.Severity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "log_groups")
public class LogGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String fingerprint;

    @Column(columnDefinition = "TEXT")
    private String firstMessage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity;

    private Long count;
    private Instant firstSeenAt;
    private Instant lastSeenAt;
    private Long sourceId;

    /** Lifecycle (P10 / 2.1). Written by SQL (LogGroupingService, LogGroupLifecycleService). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private GroupStatus status;
    private Instant resolvedAt;
    @Column(length = 100)
    private String resolvedBy;
    private Instant ignoreUntil;
    private Instant regressedAt;
    @Column(nullable = false)
    private Integer regressionCount;

    /** Collaboration (P10 / 2.2). Username of the person working on it. */
    @Column(length = 100)
    private String assignee;
    private Instant assignedAt;
    /** New entries go to this group (processor); null = not merged. */
    private Long mergedIntoId;
    private Instant mergedAt;

    /**
     * Initializes defaults before persisting.
     */
    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        if (count == null) {
            count = 1L;
        }
        if (firstSeenAt == null) {
            firstSeenAt = now;
        }
        if (lastSeenAt == null) {
            lastSeenAt = now;
        }
        if (status == null) {
            status = GroupStatus.UNRESOLVED;
        }
        if (regressionCount == null) {
            regressionCount = 0;
        }
    }
}
