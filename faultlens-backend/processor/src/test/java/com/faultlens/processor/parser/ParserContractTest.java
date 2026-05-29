package com.faultlens.processor.parser;

import static org.assertj.core.api.Assertions.assertThat;

import com.faultlens.common.dto.LogEventDto;
import com.faultlens.common.enums.Severity;
import java.util.List;
import org.junit.jupiter.api.Test;

class ParserContractTest {
    @Test
    void registryOrdersParsersByPriority() {
        PatternRegistry registry = new PatternRegistry(List.of(
                new GenericLogParser(),
                new NginxLogParser(),
                new JavaStackTraceParser(),
                new SpringBootLogParser()));
        registry.afterPropertiesSet();

        assertThat(registry.getParsers())
                .extracting(LogParser::getPriority)
                .containsExactly(10, 15, 20, 999);
    }

    @Test
    void parsesJavaStackTrace() {
        LogEventDto event = LogEventDto.builder()
                .message("java.lang.NullPointerException\n\tat com.example.PaymentService.pay(PaymentService.java:42)")
                .build();

        ParsedLog parsed = new JavaStackTraceParser().parse(event);

        assertThat(parsed.severity()).isEqualTo(Severity.ERROR);
        assertThat(parsed.parsedMessage()).isEqualTo("java.lang.NullPointerException");
        assertThat(parsed.stackTrace()).contains("PaymentService.java:42");
    }

    @Test
    void parsesSpringBootLogLine() {
        LogEventDto event = LogEventDto.builder()
                .rawLine("2024-01-15 14:23:01.123 ERROR 123 --- [main] com.example.PaymentService : Payment failed")
                .build();

        ParsedLog parsed = new SpringBootLogParser().parse(event);

        assertThat(parsed.severity()).isEqualTo(Severity.ERROR);
        assertThat(parsed.parsedMessage()).isEqualTo("Payment failed");
        assertThat(parsed.metadata()).containsEntry("format", "spring-boot");
        assertThat(parsed.metadata()).containsEntry("thread", "main");
    }

    @Test
    void parsesNginxCombinedLogLine() {
        LogEventDto event = LogEventDto.builder()
                .rawLine("10.0.0.1 - - [15/Jan/2024:14:23:01 +0000] \"GET /checkout HTTP/1.1\" 503 12")
                .build();

        ParsedLog parsed = new NginxLogParser().parse(event);

        assertThat(parsed.severity()).isEqualTo(Severity.ERROR);
        assertThat(parsed.parsedMessage()).isEqualTo("GET /checkout -> 503");
        assertThat(parsed.metadata()).containsEntry("ip", "10.0.0.1");
        assertThat(parsed.metadata()).containsEntry("path", "/checkout");
    }

    @Test
    void genericParserFindsWarnKeyword() {
        LogEventDto event = LogEventDto.builder().message("WARN connection is slow").build();

        ParsedLog parsed = new GenericLogParser().parse(event);

        assertThat(parsed.severity()).isEqualTo(Severity.WARN);
    }
}
