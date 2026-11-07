package com.faultlens.api.ingest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.faultlens.collector.kafka.LogEventProducer;
import com.faultlens.collector.model.LogSource;
import com.faultlens.collector.repository.LogSourceRepository;
import com.faultlens.collector.service.CollectorOrchestrator;
import com.faultlens.common.dto.LogEventDto;
import com.faultlens.common.enums.LogSourceType;
import com.faultlens.common.enums.Severity;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

class IngestServiceTest {
    private final LogEventProducer producer = mock(LogEventProducer.class);
    private final LogSourceRepository sources = mock(LogSourceRepository.class);
    private final CollectorOrchestrator orchestrator = mock(CollectorOrchestrator.class);
    private final IngestService service = new IngestService(producer, sources, orchestrator, 3, 200, 1000);

    private final LogSource http = new LogSource();

    @BeforeEach
    void setUp() {
        http.setId(5L);
        http.setName("orders-http");
        http.setType(LogSourceType.HTTP);
        http.setEnabled(true);
        when(sources.findById(5L)).thenReturn(Optional.of(http));
        when(producer.send(any())).thenReturn(CompletableFuture.completedFuture(null));
    }

    private static PushedLog log(String message) {
        return new PushedLog(null, Severity.ERROR, message, null, "orders", null, null, null, null, Map.of());
    }

    @Test
    void publishesEachRecordUnderTheKeysSource() {
        PushedLog record = new PushedLog(null, null, "Payment failed", "java.lang.X: y\n\tat a.B.c(B.java:1)",
                "orders", "prod", "pod-1", "app", "eu", Map.of("sourceId", "999", "stream", "stderr", "order", "42"));

        assertThat(service.ingest(5L, List.of(record, log("second")), "json")).isEqualTo(2);

        ArgumentCaptor<LogEventDto> sent = ArgumentCaptor.forClass(LogEventDto.class);
        verify(producer, times(2)).send(sent.capture());
        LogEventDto event = sent.getAllValues().getFirst();
        assertThat(event.getLabels()).containsEntry("sourceId", "5")   // not the client's 999
                .containsEntry("order", "42").containsEntry("ingest", "json").doesNotContainKey("stream");
        assertThat(event.getMessage()).isEqualTo("Payment failed\njava.lang.X: y\n\tat a.B.c(B.java:1)");
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getSource()).isEqualTo("orders-http");
        verify(orchestrator).recordPushed(5L);
    }

    @Test
    void truncatesVeryLongMessages() {
        LogEventDto event = service.toEvent(http, log("x".repeat(500)), "json");

        assertThat(event.getMessage()).hasSize(201).endsWith("…");
    }

    @Test
    void rejectsWholeRequestsBeforeWritingAnything() {
        assertThatThrownBy(() -> service.ingest(5L, List.of(log("a"), log("b"), log("c"), log("d")), "json"))
                .isInstanceOfSatisfying(IngestRejectedException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONTENT_TOO_LARGE));
        assertThatThrownBy(() -> service.ingest(5L, List.of(log("a"), log(" ")), "json"))
                .hasMessageContaining("Record 1");
        verify(producer, never()).send(any());
    }

    @Test
    void refusesKeysWithoutAUsableSource() {
        assertThatThrownBy(() -> service.ingest(null, List.of(log("a")), "json"))
                .isInstanceOfSatisfying(IngestRejectedException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo("API_KEY_NO_SOURCE"));

        http.setEnabled(false);
        assertThatThrownBy(() -> service.ingest(5L, List.of(log("a")), "json"))
                .isInstanceOfSatisfying(IngestRejectedException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT));

        http.setEnabled(true);
        http.setType(LogSourceType.SSH);
        assertThatThrownBy(() -> service.ingest(5L, List.of(log("a")), "json"))
                .isInstanceOfSatisfying(IngestRejectedException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void answers503WhenKafkaDoesNotAcknowledge() {
        when(producer.send(any())).thenReturn(CompletableFuture.failedFuture(new IllegalStateException("down")));

        assertThatThrownBy(() -> service.ingest(5L, List.of(log("a")), "otlp"))
                .isInstanceOfSatisfying(IngestRejectedException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(ex.getRetryAfterSeconds()).isEqualTo(5L);
                });
        verify(orchestrator, never()).recordPushed(any());
    }

    @Test
    void parsesCommonSeverityNames() {
        assertThat(IngestService.parseSeverity("warning")).isEqualTo(Severity.WARN);
        assertThat(IngestService.parseSeverity("FATAL")).isEqualTo(Severity.CRITICAL);
        assertThat(IngestService.parseSeverity("severe")).isEqualTo(Severity.ERROR);
        assertThat(IngestService.parseSeverity("loud")).isNull();
        assertThat(IngestService.parseSeverity(null)).isNull();
    }
}
