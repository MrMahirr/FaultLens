package com.loglens.processor.kafka;

import com.loglens.common.dto.LogEntryDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogErrorProducer {
    private final KafkaTemplate<String, LogEntryDto> logEntryKafkaTemplate;

    /**
     * Publishes error logs for analyzer processing and realtime UI.
     */
    public void send(LogEntryDto entry) {
        logEntryKafkaTemplate.send("log-errors", String.valueOf(entry.getGroupId()), entry)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send error log {}", entry.getId(), ex);
                    }
                });
    }
}
