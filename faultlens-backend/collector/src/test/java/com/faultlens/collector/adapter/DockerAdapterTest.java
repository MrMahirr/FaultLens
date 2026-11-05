package com.faultlens.collector.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.faultlens.collector.model.LogSource;
import com.faultlens.common.enums.LogSourceType;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class DockerAdapterTest {
    @Test
    void newSourceReadsTheExistingLogOnce() {
        assertThat(DockerAdapter.sinceEpochSeconds(source(null))).isNull();
    }

    @Test
    void knownSourceResumesFromItsLastLineInsteadOfReplayingTheWholeLog() {
        Instant lastSeen = Instant.parse("2026-11-05T01:02:03.900Z");

        assertThat(DockerAdapter.sinceEpochSeconds(source(lastSeen))).isEqualTo((int) lastSeen.getEpochSecond());
    }

    private static LogSource source(Instant lastSeen) {
        return LogSource.builder()
                .id(2L)
                .name("docker")
                .type(LogSourceType.DOCKER)
                .config("{\"containerId\":\"abc\"}")
                .lastSeenAt(lastSeen)
                .build();
    }
}
