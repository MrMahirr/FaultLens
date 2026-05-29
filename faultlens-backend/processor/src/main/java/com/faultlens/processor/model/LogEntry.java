package com.faultlens.processor.model;

import com.faultlens.common.enums.Severity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
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
@Table(
        name = "log_entries",
        indexes = {
                @Index(name = "idx_log_entries_timestamp", columnList = "timestamp DESC"),
                @Index(name = "idx_log_entries_severity", columnList = "severity"),
                @Index(name = "idx_log_entries_group", columnList = "group_id"),
                @Index(name = "idx_log_entries_source_ts", columnList = "source_id,timestamp DESC")
        }
)
public class LogEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long sourceId;
    private Long groupId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(columnDefinition = "TEXT")
    private String parsedMessage;

    @Column(columnDefinition = "TEXT")
    private String stackTrace;

    @Column(columnDefinition = "TEXT")
    private String rawLine;

    private String namespace;
    private String podName;
    private String containerName;
    private String serviceName;
    private String cluster;

    @Column(nullable = false)
    private Instant timestamp;

    private Instant createdAt;

    /**
     * Initializes created timestamp in UTC.
     */
    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
