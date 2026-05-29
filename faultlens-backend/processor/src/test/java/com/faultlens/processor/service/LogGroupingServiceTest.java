package com.faultlens.processor.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.faultlens.common.enums.Severity;
import org.junit.jupiter.api.Test;

class LogGroupingServiceTest {
    private final LogGroupingService service = new LogGroupingService(null, null);

    @Test
    void stackTraceFingerprintIgnoresOuterWhitespace() {
        String first = service.fingerprint("message", " java.lang.Error\n\tat com.example.App.run(App.java:1) ", Severity.ERROR);
        String second = service.fingerprint("other", "java.lang.Error\n\tat com.example.App.run(App.java:1)", Severity.ERROR);

        assertThat(first).isEqualTo(second);
    }

    @Test
    void messageFingerprintIncludesSeverityAndFirstTwoHundredCharacters() {
        String message = "x".repeat(250);

        String first = service.fingerprint(message, null, Severity.ERROR);
        String second = service.fingerprint(message.substring(0, 200) + "different-tail", null, Severity.ERROR);
        String third = service.fingerprint(message, null, Severity.WARN);

        assertThat(first).isEqualTo(second);
        assertThat(first).isNotEqualTo(third);
    }
}
