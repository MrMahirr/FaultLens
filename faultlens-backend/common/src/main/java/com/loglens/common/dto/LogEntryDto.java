package com.loglens.common.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.loglens.common.enums.Severity;
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
public class LogEntryDto {
    private Long id;
    @JsonAlias("sourceId")
    private Long sourceId;
    @JsonAlias("groupId")
    private Long groupId;
    private Severity severity;
    private String message;
    @JsonAlias("parsedMessage")
    private String parsedMessage;
    @JsonAlias("stackTrace")
    private String stackTrace;
    private String namespace;
    @JsonAlias("podName")
    private String podName;
    @JsonAlias("containerName")
    private String containerName;
    @JsonAlias("serviceName")
    private String serviceName;
    private String cluster;
    private Instant timestamp;
}
