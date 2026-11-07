package com.faultlens.api.dto;

import com.faultlens.api.model.AlarmChannel;
import com.faultlens.api.model.AlarmRuleType;
import com.faultlens.common.enums.Severity;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

public final class AlarmRuleDtos {
    private AlarmRuleDtos() {
    }

    /**
     * Create/update payload. Type-specific fields are checked by AlarmRuleService (threshold and
     * window for THRESHOLD, a compilable pattern for PATTERN).
     */
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record AlarmRuleRequest(
            @NotBlank @Size(max = 200) String name,
            @NotNull AlarmRuleType type,
            @NotNull Severity severity,
            @Min(60) @Max(3600) Integer windowSec,
            @Min(1) @Max(1_000_000) Integer threshold,
            @Size(max = 500) String pattern,
            List<Long> sourceIds,
            List<AlarmChannel> channels,
            @Min(0) @Max(86_400) Integer cooldownSec,
            Boolean enabled) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record AlarmRuleResponse(
            Long id,
            String name,
            AlarmRuleType type,
            Severity severity,
            Integer windowSec,
            Integer threshold,
            String pattern,
            List<Long> sourceIds,
            List<AlarmChannel> channels,
            Integer cooldownSec,
            boolean enabled,
            String createdBy,
            Instant createdAt,
            Instant updatedAt) {
    }
}
