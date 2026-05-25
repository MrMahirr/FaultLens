package com.loglens.api.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.loglens.analyzer.model.Analysis;
import com.loglens.analyzer.model.Deployment;
import com.loglens.api.dto.DeploymentDto;
import com.loglens.collector.model.LogSource;
import com.loglens.common.enums.LogSourceType;
import com.loglens.common.enums.Severity;
import com.loglens.processor.model.LogEntry;
import com.loglens.processor.model.LogGroup;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ApiDtoMapperTest {
    private final ApiDtoMapper mapper = new ApiDtoMapper();

    @Test
    void mapsLogEntryWithoutEntityFields() {
        LogEntry entry = LogEntry.builder()
                .id(1L)
                .sourceId(2L)
                .groupId(3L)
                .severity(Severity.ERROR)
                .message("message")
                .parsedMessage("parsed")
                .stackTrace("stack")
                .namespace("default")
                .podName("pod")
                .containerName("container")
                .serviceName("service")
                .cluster("cluster")
                .timestamp(Instant.parse("2024-01-15T14:23:01Z"))
                .build();

        var dto = mapper.toLogEntryDto(entry);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getSourceId()).isEqualTo(2L);
        assertThat(dto.getParsedMessage()).isEqualTo("parsed");
        assertThat(dto.getStackTrace()).isEqualTo("stack");
    }

    @Test
    void mapsLogGroupToDto() {
        LogGroup group = LogGroup.builder()
                .id(10L)
                .fingerprint("abc")
                .firstMessage("first")
                .severity(Severity.WARN)
                .count(4L)
                .build();

        var dto = mapper.toLogGroupDto(group);

        assertThat(dto.getId()).isEqualTo(10L);
        assertThat(dto.getFingerprint()).isEqualTo("abc");
        assertThat(dto.getCount()).isEqualTo(4L);
    }

    @Test
    void mapsAnalysisToDto() {
        Analysis analysis = Analysis.builder()
                .id(1L)
                .logGroupId(2L)
                .logEntryId(3L)
                .rootCause("cause")
                .suggestion("suggest")
                .engineType("RULE_BASED")
                .confidenceScore(0.9)
                .build();

        var dto = mapper.toAnalysisDto(analysis);

        assertThat(dto.getLogEntryId()).isEqualTo(3L);
        assertThat(dto.getEngineType()).isEqualTo("RULE_BASED");
    }

    @Test
    void mapsSourceToDto() {
        LogSource source = LogSource.builder()
                .id(5L)
                .name("source")
                .type(LogSourceType.DOCKER)
                .config("{}")
                .enabled(true)
                .build();

        var dto = mapper.toLogSourceDto(source);

        assertThat(dto.getId()).isEqualTo(5L);
        assertThat(dto.getType()).isEqualTo(LogSourceType.DOCKER);
        assertThat(dto.isEnabled()).isTrue();
    }

    @Test
    void mapsDeploymentBothWays() {
        DeploymentDto dto = DeploymentDto.builder()
                .serviceName("payment")
                .version("1.2.3")
                .environment("production")
                .deployedAt(Instant.parse("2024-01-15T14:23:01Z"))
                .deployedBy("ci")
                .status("SUCCESS")
                .notes("release")
                .build();

        Deployment entity = mapper.toDeploymentEntity(dto);
        DeploymentDto mapped = mapper.toDeploymentDto(entity);

        assertThat(mapped.getServiceName()).isEqualTo("payment");
        assertThat(mapped.getNotes()).isEqualTo("release");
    }
}
