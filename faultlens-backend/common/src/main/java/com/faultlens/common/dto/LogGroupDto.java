package com.faultlens.common.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.faultlens.common.enums.Severity;
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
public class LogGroupDto {
    private Long id;
    private String fingerprint;
    private String firstMessage;
    private Severity severity;
    private Long count;
    private Instant firstSeenAt;
    private Instant lastSeenAt;
}
