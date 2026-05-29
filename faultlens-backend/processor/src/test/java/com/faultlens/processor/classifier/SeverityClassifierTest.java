package com.faultlens.processor.classifier;

import static org.assertj.core.api.Assertions.assertThat;

import com.faultlens.common.dto.LogEventDto;
import com.faultlens.common.enums.Severity;
import com.faultlens.processor.parser.ParsedLog;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SeverityClassifierTest {
    private final SeverityClassifier classifier = new SeverityClassifier();

    @Test
    void returnsHigherSeverityBetweenEventAndParser() {
        LogEventDto event = LogEventDto.builder().severity(Severity.WARN).build();
        ParsedLog parsed = new ParsedLog("message", null, Severity.ERROR, Map.of());

        assertThat(classifier.classify(event, parsed)).isEqualTo(Severity.ERROR);
    }

    @Test
    void treatsStderrAsMinimumWarn() {
        LogEventDto event = LogEventDto.builder()
                .severity(Severity.INFO)
                .labels(Map.of("stream", "STDERR"))
                .build();
        ParsedLog parsed = new ParsedLog("message", null, Severity.INFO, Map.of());

        assertThat(classifier.classify(event, parsed)).isEqualTo(Severity.WARN);
    }
}
