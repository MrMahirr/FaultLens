package com.loglens.collector.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.loglens.collector.dto.LogSourceCreateRequest;
import com.loglens.collector.dto.LogSourceUpdateRequest;
import com.loglens.collector.model.LogSource;
import com.loglens.common.enums.LogSourceType;
import org.junit.jupiter.api.Test;

class LogSourceMapperTest {
    private final LogSourceMapper mapper = new LogSourceMapper();

    @Test
    void mapsCreateRequestToEntity() {
        LogSourceCreateRequest request = LogSourceCreateRequest.builder()
                .name("k8s")
                .type(LogSourceType.KUBERNETES)
                .config("{}")
                .enabled(true)
                .build();

        LogSource source = mapper.toEntity(request);

        assertThat(source.getName()).isEqualTo("k8s");
        assertThat(source.getType()).isEqualTo(LogSourceType.KUBERNETES);
        assertThat(source.getConfig()).isEqualTo("{}");
        assertThat(source.isEnabled()).isTrue();
    }

    @Test
    void appliesUpdateRequestToExistingEntity() {
        LogSource source = LogSource.builder()
                .name("old")
                .type(LogSourceType.SSH)
                .enabled(true)
                .build();
        LogSourceUpdateRequest request = LogSourceUpdateRequest.builder()
                .name("docker")
                .type(LogSourceType.DOCKER)
                .config("{\"containerId\":\"abc\"}")
                .enabled(false)
                .build();

        mapper.updateEntity(source, request);

        assertThat(source.getName()).isEqualTo("docker");
        assertThat(source.getType()).isEqualTo(LogSourceType.DOCKER);
        assertThat(source.getConfig()).isEqualTo("{\"containerId\":\"abc\"}");
        assertThat(source.isEnabled()).isFalse();
    }
}
