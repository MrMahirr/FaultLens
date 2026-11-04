package com.faultlens.processor.parser;

import static org.assertj.core.api.Assertions.assertThat;

import com.faultlens.common.dto.LogEventDto;
import com.faultlens.common.enums.Severity;
import java.util.List;
import org.junit.jupiter.api.Test;

class ParserContractTest {
    @Test
    void registryOrdersParsersByPriority() {
        assertThat(registry().getParsers())
                .extracting(LogParser::getPriority)
                .containsExactly(5, 10, 20, 999);
        assertThat(registry().getParsers().getFirst()).isInstanceOf(SpringBootLogParser.class);
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

    // ── Regression: Spring Boot 3 lines used to be classified by keyword (2026-11-05) ──

    @Test
    void parsesSpringBoot3LineWithIsoTimestampApplicationAndThread() {
        ParsedLog parsed = parseWithRegistry(
                "2026-06-05T21:12:22.845Z  INFO 1 --- [ruleengine] [ionShutdownHook] com.zaxxer.hikari.HikariDataSource"
                        + "       : HikariPool-1 - Shutdown completed.");

        assertThat(parsed.severity()).isEqualTo(Severity.INFO);
        assertThat(parsed.parsedMessage()).isEqualTo("HikariPool-1 - Shutdown completed.");
        assertThat(parsed.metadata())
                .containsEntry("format", "spring-boot")
                .containsEntry("application", "ruleengine")
                .containsEntry("thread", "ionShutdownHook")
                .containsEntry("logger", "com.zaxxer.hikari.HikariDataSource");
        assertThat(parsed.stackTrace()).isNull();
    }

    @Test
    void warnLineMentioningAnErrorStaysWarn() {
        // Shape of a real line that was stored as ERROR: keyword matching saw "Error" / "at ".
        ParsedLog parsed = parseWithRegistry(
                "2026-06-05T19:02:36.647Z  WARN 1 --- [ruleengine] [nio-3600-exec-7] "
                        + "ration$PageModule$WarningLoggingModifier : Serializing PageImpl instances as-is is not "
                        + "supported. Error handling at this level is up to you.");

        assertThat(parsed.severity()).isEqualTo(Severity.WARN);
        assertThat(parsed.metadata()).containsEntry("format", "spring-boot");
    }

    @Test
    void springBootErrorLineKeepsItsStackTrace() {
        ParsedLog parsed = parseWithRegistry(
                "2026-06-05T19:02:36.647+03:00 ERROR 1 --- [main] c.e.PaymentService : Payment failed\n"
                        + "java.lang.IllegalStateException: boom\n"
                        + "\tat com.example.PaymentService.pay(PaymentService.java:42)");

        assertThat(parsed.severity()).isEqualTo(Severity.ERROR);
        assertThat(parsed.parsedMessage()).isEqualTo("Payment failed");
        assertThat(parsed.stackTrace())
                .startsWith("java.lang.IllegalStateException: boom")
                .contains("PaymentService.java:42");
    }

    @Test
    void javaParserNeedsARealStackTrace() {
        JavaStackTraceParser parser = new JavaStackTraceParser();

        assertThat(parser.supports(event("request handled at 10:00, no Error reported"))).isFalse();
        assertThat(parser.supports(event("Connection reset by peer"))).isFalse();
        assertThat(parser.supports(event("java.lang.NullPointerException: x"))).isTrue();
        assertThat(parser.supports(event("Exception in thread \"main\" java.lang.OutOfMemoryError: Java heap space")))
                .isTrue();
        assertThat(parser.supports(event("boom\n\tat com.example.Foo.bar(Foo.java:1)"))).isTrue();
        assertThat(parser.supports(event("wrapper failed\nCaused by: java.io.IOException: x"))).isTrue();
    }

    @Test
    void genericParserMatchesLevelsAsWholeWords() {
        GenericLogParser parser = new GenericLogParser();

        assertThat(parser.parse(event("stats errorCount=0 warnings_total=0")).severity()).isEqualTo(Severity.INFO);
        assertThat(parser.parse(event("[error] upstream timed out")).severity()).isEqualTo(Severity.ERROR);
        assertThat(parser.parse(event("WARNING: disk almost full")).severity()).isEqualTo(Severity.WARN);
        assertThat(parser.parse(event("fatal: repository not found")).severity()).isEqualTo(Severity.CRITICAL);
    }

    private static PatternRegistry registry() {
        PatternRegistry registry = new PatternRegistry(List.of(
                new GenericLogParser(),
                new NginxLogParser(),
                new JavaStackTraceParser(),
                new SpringBootLogParser()));
        registry.afterPropertiesSet();
        return registry;
    }

    private static ParsedLog parseWithRegistry(String rawLine) {
        LogEventDto event = LogEventDto.builder().rawLine(rawLine).message(rawLine).build();
        return registry().selectParser(event).parse(event);
    }

    private static LogEventDto event(String text) {
        return LogEventDto.builder().message(text).build();
    }
}
