package com.faultlens.api.dto;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;
import com.faultlens.api.model.AlarmStatus;
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
public class AlarmDto {
    private Long id;
    private String ruleId;
    private String ruleName;
    private Long sourceId;
    private String severity;
    /** Rule that raised the alarm; null for built-in sources (rule_id e.g. AI_ANALYSIS). */
    private Long alarmRuleId;
    private AlarmStatus status;
    private String message;
    private Instant triggeredAt;
    private Instant resolvedAt;
}
