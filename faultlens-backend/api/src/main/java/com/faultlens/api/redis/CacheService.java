package com.faultlens.api.redis;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.faultlens.api.ws.LogWebSocketHandler;
import com.faultlens.common.dto.LogEntryDto;
import java.time.Duration;
import java.util.Map;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService implements MessageListener {
    public static final String REALTIME_CHANNEL = "log-realtime";
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final LogWebSocketHandler webSocketHandler;

    /**
     * Returns cached dashboard stats or computes and stores them for five minutes.
     */
    public Map<String, Long> getStats(String sourceId, String window, Supplier<Map<String, Long>> supplier) {
        String key = "faultlens:stats:%s:%s".formatted(sourceId == null ? "all" : sourceId, window);
        return getStatsByKey(key, supplier);
    }

    /**
     * Returns cached stats for a window in minutes.
     */
    public Map<String, Long> getStats(int windowMinutes, Supplier<Map<String, Long>> supplier) {
        return getStatsByKey("faultlens:stats:" + windowMinutes, supplier);
    }

    private Map<String, Long> getStatsByKey(String key, Supplier<Map<String, Long>> supplier) {
        try {
            String cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                return objectMapper.readValue(cached, new TypeReference<>() {});
            }
            Map<String, Long> value = supplier.get();
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), Duration.ofMinutes(5));
            return value;
        } catch (Exception ex) {
            log.warn("Stats cache failed, using live value", ex);
            return supplier.get();
        }
    }

    /**
     * Publishes an error log to the realtime Redis channel.
     */
    public void publishRealtime(LogEntryDto entry) {
        try {
            redisTemplate.convertAndSend(REALTIME_CHANNEL, objectMapper.writeValueAsString(entry));
        } catch (Exception ex) {
            log.error("Could not publish realtime log", ex);
        }
    }

    /**
     * Receives Redis pub/sub messages and forwards them to WebSocket sessions.
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        webSocketHandler.broadcastRaw(new String(message.getBody()));
    }
}
