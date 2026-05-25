package com.loglens.collector.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.loglens.common.dto.LogEventDto;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

class LogEventProducerTest {
    @Test
    void sendsWithPodNameAsKeyWhenAvailable() {
        KafkaTemplate<String, LogEventDto> kafkaTemplate = mock(KafkaTemplate.class);
        LogEventDto event = LogEventDto.builder().id("event-1").podName("pod-a").build();
        CompletableFuture<SendResult<String, LogEventDto>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send("log-raw", "pod-a", event)).thenReturn(future);

        LogEventProducer producer = new LogEventProducer(kafkaTemplate);

        assertThat(producer.send(event)).isSameAs(future);
        verify(kafkaTemplate).send(eq("log-raw"), eq("pod-a"), eq(event));
    }

    @Test
    void fallsBackToSourceIdLabelWhenPodNameMissing() {
        KafkaTemplate<String, LogEventDto> kafkaTemplate = mock(KafkaTemplate.class);
        LogEventDto event = LogEventDto.builder()
                .id("event-1")
                .labels(Map.of("sourceId", "42"))
                .build();
        CompletableFuture<SendResult<String, LogEventDto>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send("log-raw", "42", event)).thenReturn(future);

        LogEventProducer producer = new LogEventProducer(kafkaTemplate);

        assertThat(producer.send(event)).isSameAs(future);
        verify(kafkaTemplate).send(eq("log-raw"), eq("42"), eq(event));
    }
}
