package com.faultlens.api.model;

import com.faultlens.common.enums.Severity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * An operator-defined alarm rule (ADR 0001). Evaluated on every processed log entry.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "alarm_rules")
public class AlarmRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlarmRuleType type;

    /** Minimum severity an entry (or new group) needs to count for the rule. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity;

    /** THRESHOLD only. */
    @Column(name = "window_sec")
    private Integer windowSec;

    /** THRESHOLD only. */
    private Integer threshold;

    /** PATTERN only: Java regular expression matched against the message. */
    private String pattern;

    /** Empty = all sources. */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "source_ids", nullable = false)
    @Builder.Default
    private List<Long> sourceIds = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(nullable = false)
    @Builder.Default
    private List<AlarmChannel> channels = new ArrayList<>(List.of(AlarmChannel.IN_APP));

    @Column(name = "cooldown_sec", nullable = false)
    @Builder.Default
    private Integer cooldownSec = 900;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** True if entries of this source count for the rule. */
    public boolean appliesTo(Long sourceId) {
        return sourceIds == null || sourceIds.isEmpty() || (sourceId != null && sourceIds.contains(sourceId));
    }

    /** True if an entry of this severity counts for the rule. */
    public boolean accepts(Severity entrySeverity) {
        return entrySeverity != null && severity != null && entrySeverity.getLevel() >= severity.getLevel();
    }
}
