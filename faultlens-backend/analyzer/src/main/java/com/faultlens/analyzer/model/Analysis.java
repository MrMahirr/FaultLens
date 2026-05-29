package com.faultlens.analyzer.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
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
@Table(name = "analyses")
public class Analysis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long logGroupId;
    private Long logEntryId;
    @Column(columnDefinition = "TEXT")
    private String rootCause;
    @Column(columnDefinition = "TEXT")
    private String suggestion;
    private String affectedDeployment;
    private Double confidenceScore;
    private String engineType;
    private Instant analyzedAt;

    /**
     * Initializes analysis timestamp.
     */
    @PrePersist
    public void prePersist() {
        if (analyzedAt == null) {
            analyzedAt = Instant.now();
        }
    }
}
