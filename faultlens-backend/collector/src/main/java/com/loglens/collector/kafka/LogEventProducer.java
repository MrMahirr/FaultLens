package com.loglens.collector.kafka;

import com.loglens.common.dto.LogEventDto;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LogEventProducer {
    private final KafkaTemplate<String, LogEventDto> kafkaTemplate;

    public LogEventProducer(@Qualifier("logEventKafkaTemplate") KafkaTemplate<String, LogEventDto> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Sends a raw log event to Kafka asynchronously.
     */
    public CompletableFuture<SendResult<String, LogEventDto>> send(LogEventDto event) {
        String key = keyFor(event);
        CompletableFuture<SendResult<String, LogEventDto>> future = kafkaTemplate.send("log-raw", key, event);
        future
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send raw log event {}", event.getId(), ex);
                    }
                });
        return future;
    }

    private String keyFor(LogEventDto event) {
        if (event.getPodName() != null && !event.getPodName().isBlank()) {
            return event.getPodName();
        }
        if (event.getLabels() != null && event.getLabels().containsKey("sourceId")) {
            return event.getLabels().get("sourceId");
        }
        return event.getId();
    }
}
