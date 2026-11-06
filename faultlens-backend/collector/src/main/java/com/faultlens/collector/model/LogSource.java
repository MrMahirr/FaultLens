package com.faultlens.collector.model;

import com.faultlens.common.enums.LogSourceType;
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
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "log_sources")
public class LogSource {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LogSourceType type;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @ToString.Exclude // may contain credentials
    private String config;

    @Builder.Default
    @Column(nullable = false)
    private boolean enabled = true;

    private Instant createdAt;
    private Instant lastSeenAt;

    /*
     * Collector health (P08, V20). Written only by SourceHealthReporter with targeted UPDATEs,
     * never by save(entity): an edit of the source must not overwrite a newer health state.
     */
    @Column(insertable = false, updatable = false)
    private String status;

    @Column(insertable = false, updatable = false)
    private String lastError;

    @Column(insertable = false, updatable = false)
    private Instant lastErrorAt;

    @Column(insertable = false, updatable = false)
    private String collectorInstance;

    /**
     * Initializes UTC timestamps before insert.
     */
    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
