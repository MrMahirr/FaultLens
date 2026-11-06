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

    @Test
    void linesThatDifferOnlyInVariablePartsShareAGroup() {
        assertThat(service.fingerprint("GET /api/v1/orders/123 200 in 45ms", null, Severity.INFO))
                .isEqualTo(service.fingerprint("GET /api/v1/orders/98765 200 in 3ms", null, Severity.INFO));
        assertThat(service.fingerprint("Connection refused: db-1.internal:5432 after 3001ms", null, Severity.ERROR))
                .isEqualTo(service.fingerprint("Connection refused: db-7.internal:5432 after 12ms", null, Severity.ERROR));
        assertThat(service.fingerprint("Connection refused", null, Severity.ERROR))
                .isNotEqualTo(service.fingerprint("Connection reset", null, Severity.ERROR));
    }

    @Test
    void stackTracesWithDifferentExceptionValuesShareAGroup() {
        String a = "java.lang.IllegalStateException: order 17 already shipped\n\tat com.example.Orders.ship(Orders.java:42)";
        String b = "java.lang.IllegalStateException: order 9001 already shipped\n\tat com.example.Orders.ship(Orders.java:42)";
        assertThat(service.fingerprint("x", a, Severity.ERROR)).isEqualTo(service.fingerprint("y", b, Severity.ERROR));
    }
}
