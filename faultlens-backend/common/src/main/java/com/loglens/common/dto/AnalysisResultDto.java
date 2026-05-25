package com.loglens.common.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
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
public class AnalysisResultDto {
    private Long id;
    @JsonAlias("logGroupId")
    private Long logGroupId;
    @JsonAlias("logEntryId")
    private Long logEntryId;
    @JsonAlias("rootCause")
    private String rootCause;
    private String suggestion;
    @JsonAlias("affectedDeployment")
    private String affectedDeployment;
    @JsonAlias("confidenceScore")
    private Double confidenceScore;
    @JsonAlias("engineType")
    private String engineType;
    @JsonAlias("analyzedAt")
    private Instant analyzedAt;
}
