package com.faultlens.api.dto;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlarmDto {
    private Long id;
    private String ruleId;
    private String ruleName;
    private Long sourceId;
    private String severity;
    private String status;
    private String message;
    private Instant triggeredAt;
    private Instant resolvedAt;
}
