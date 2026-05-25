package com.loglens.api.service;

import com.loglens.api.redis.CacheService;
import com.loglens.common.dto.LogEntryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RealtimeLogConsumer {
    private final CacheService cacheService;

    /**
     * Bridges error logs from Kafka to Redis pub/sub for WebSocket fanout.
     */
    @KafkaListener(topics = "log-errors", groupId = "loglens-api-realtime", containerFactory = "logEntryKafkaListenerContainerFactory")
    public void consume(LogEntryDto entry) {
        cacheService.publishRealtime(entry);
    }
}
