package com.faultlens.common.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.faultlens.common.enums.Severity;
import java.time.Instant;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class LogEventDto {
    private String id;
    private String source;
    private String cluster;
    private String namespace;
    @JsonAlias("podName")
    private String podName;
    @JsonAlias("containerName")
    private String containerName;
    @JsonAlias("nodeName")
    private String nodeName;
    @JsonAlias("serviceName")
    private String serviceName;
    private Severity severity;
    private String message;
    @JsonAlias("rawLine")
    private String rawLine;
    private Instant timestamp;
    private Map<String, String> labels;
}
