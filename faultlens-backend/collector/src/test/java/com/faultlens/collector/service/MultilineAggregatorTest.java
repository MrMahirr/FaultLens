package com.faultlens.collector.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.faultlens.common.dto.LogEventDto;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class MultilineAggregatorTest {
    private final List<LogEventDto> sent = new ArrayList<>();
    private final AtomicLong nanos = new AtomicLong();
    private final MultilineAggregator aggregator = new MultilineAggregator(sent::add, nanos::get);

    @Test
    void joinsASpringBootErrorWithItsStackTrace() {
        feed("2026-11-05T10:00:00.000Z ERROR 1 --- [main] c.e.PaymentService : Payment failed",
                "java.lang.IllegalStateException: boom",
                "\tat com.example.PaymentService.pay(PaymentService.java:42)",
                "\tat com.example.Api.call(Api.java:7)",
                "Caused by: java.io.IOException: reset",
                "\t... 12 more",
                "2026-11-05T10:00:01.000Z  INFO 1 --- [main] c.e.PaymentService : Retrying");
        aggregator.flush();

        assertThat(sent).hasSize(2);
        assertThat(sent.get(0).getRawLine())
                .startsWith("2026-11-05T10:00:00.000Z ERROR")
                .contains("\njava.lang.IllegalStateException: boom\n\tat com.example.PaymentService.pay")
                .endsWith("\t... 12 more");
        assertThat(sent.get(0).getMessage()).isEqualTo(sent.get(0).getRawLine());
        assertThat(sent.get(1).getRawLine()).endsWith("Retrying");
    }

    @Test
    void joinsAWarningWrappedOverSeveralLines() {
        // Shape of the Spring Data warning that was stored as three separate INFO entries.
        feed("2026-06-05T19:02:36.647Z  WARN 1 --- [ruleengine] [nio-exec-7] o.s.d.w.c.SpringDataJacksonConfiguration : "
                        + "Serializing PageImpl instances as-is is not supported, meaning that there is no guarantee",
                "For a stable JSON structure, please use Spring Data's PagedModel (globally via",
                "or Spring HATEOAS and Spring Data's PagedResourcesAssembler as documented in https://docs.spring.io/");
        aggregator.flush();

        assertThat(sent).hasSize(1);
        assertThat(sent.getFirst().getRawLine().lines()).hasSize(3);
    }

    @Test
    void joinsAPlainJavaStackTraceWithoutTimestamps() {
        feed("Exception in thread \"main\" java.lang.NullPointerException: x",
                "\tat com.example.Main.main(Main.java:3)",
                "plain unrelated line");
        aggregator.flush();

        assertThat(sent).extracting(LogEventDto::getRawLine).containsExactly(
                "Exception in thread \"main\" java.lang.NullPointerException: x\n\tat com.example.Main.main(Main.java:3)",
                "plain unrelated line");
    }

    @Test
    void keepsIndependentLinesSeparateWhenTheSourceIsConsistent() {
        // Every line timestamped, or none: nothing is joined.
        feed("2026-11-05T10:00:00Z INFO started", "2026-11-05T10:00:01Z INFO ready");
        aggregator.flush();
        feed("GET /health 200", "GET /ready 200");
        aggregator.flush();

        assertThat(sent).hasSize(4);
    }

    @Test
    void knownTradeOff_untimestampedLinesAfterATimestampedOneAreJoined() {
        // Same rule as Filebeat/Fluent Bit multiline (pattern ^timestamp, negate): needed for
        // wrapped messages, but a source that mixes timestamped and bare lines gets them joined.
        feed("2026-11-05T10:00:00Z INFO started", "GET /health 200");
        aggregator.flush();

        assertThat(sent).hasSize(1);
    }

    @Test
    void sendsTheLastEventAfterAQuietPeriod() {
        feed("2026-11-05T10:00:00Z ERROR failed", "\tat com.example.A.b(A.java:1)");

        aggregator.flushIfIdle();
        assertThat(sent).as("not idle yet").isEmpty();

        nanos.addAndGet(MultilineAggregator.IDLE.toNanos());
        aggregator.flushIfIdle();
        assertThat(sent).hasSize(1);
        assertThat(sent.getFirst().getRawLine()).contains("A.java:1");
    }

    @Test
    void capsTheNumberOfJoinedLines() {
        aggregator.accept(line("2026-11-05T10:00:00Z ERROR huge"));
        for (int i = 0; i < MultilineAggregator.MAX_LINES + 10; i++) {
            aggregator.accept(line("\tat frame" + i + "(X.java:1)"));
        }
        aggregator.flush();

        assertThat(sent).hasSize(2);
        assertThat(sent.getFirst().getRawLine().lines()).hasSize(MultilineAggregator.MAX_LINES);
    }

    @Test
    void keepsTheFirstLinesMetadata() {
        LogEventDto first = LogEventDto.builder().id("1").podName("orders-1").rawLine("2026-11-05T10:00:00Z ERROR x")
                .message("2026-11-05T10:00:00Z ERROR x").build();
        aggregator.accept(first);
        aggregator.accept(LogEventDto.builder().id("2").podName("orders-1").rawLine("\tat a.B.c(B.java:1)")
                .message("\tat a.B.c(B.java:1)").build());
        aggregator.flush();

        assertThat(sent).singleElement().satisfies(e -> {
            assertThat(e.getId()).isEqualTo("1");
            assertThat(e.getPodName()).isEqualTo("orders-1");
        });
    }

    private void feed(String... lines) {
        for (String text : lines) {
            aggregator.accept(line(text));
        }
    }

    private static LogEventDto line(String text) {
        return LogEventDto.builder().rawLine(text).message(text).build();
    }
}
