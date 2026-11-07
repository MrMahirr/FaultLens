package com.faultlens.api.dto;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;
import com.faultlens.common.enums.LogSourceType;
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
public class LogSourceDto {
    private Long id;
    private String name;
    private LogSourceType type;
    private String config;
    private boolean enabled;
    private Instant createdAt;
    private Instant lastSeenAt;
    /** Live state from the collector; set by SourceController. */
    private SourceStatus status;
    /** Collector health (P08): CONNECTED, ERROR or IDLE, shared by all instances. */
    private String health;
    /** Last adapter error (kept after recovery, for context) and when it happened. */
    private String lastError;
    private Instant lastErrorAt;
    /** Instance that collects (or last collected) the source. */
    private String collectorInstance;
}
