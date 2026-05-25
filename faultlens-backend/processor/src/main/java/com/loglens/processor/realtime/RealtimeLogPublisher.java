package com.loglens.processor.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loglens.common.dto.LogEntryDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RealtimeLogPublisher {
    public static final String REALTIME_CHANNEL = "log-realtime";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Publishes a processed log entry to the realtime Redis channel.
     */
    public void publish(LogEntryDto entry) {
        try {
            redisTemplate.convertAndSend(REALTIME_CHANNEL, objectMapper.writeValueAsString(entry));
        } catch (Exception ex) {
            log.error("Failed to publish realtime log entry {}", entry.getId(), ex);
        }
    }
}
