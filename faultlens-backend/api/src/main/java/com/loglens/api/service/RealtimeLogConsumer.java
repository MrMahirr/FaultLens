package com.loglens.api.service;

import org.springframework.stereotype.Component;

@Component
public class RealtimeLogConsumer {
    // Disabled to prevent duplicate log broadcasts.
    // LogProcessorService in the processor module already publishes all processed logs
    // directly to the Redis 'log-realtime' channel, which the api module's CacheService
    // consumes and broadcasts to WebSocket clients.
    
    /*
    private final CacheService cacheService;

    @org.springframework.kafka.annotation.KafkaListener(topics = "log-errors", groupId = "loglens-api-realtime", containerFactory = "logEntryKafkaListenerContainerFactory")
    public void consume(com.loglens.common.dto.LogEntryDto entry) {
        cacheService.publishRealtime(entry);
    }
    */
}

