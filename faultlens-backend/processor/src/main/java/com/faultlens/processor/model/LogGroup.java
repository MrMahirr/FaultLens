package com.faultlens.processor.model;

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
    }
}
