package com.faultlens.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.faultlens.api.ws.LogWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InAppNotificationService {

    private final LogWebSocketHandler logWebSocketHandler;
    private final ObjectMapper objectMapper;

    /**
     * Broadcasts an alarm/notification to all connected clients via WebSocket.
     *
     * @param title The alarm title
     * @param message The alarm details
     */
    public void broadcastAlarm(String title, String message) {
        try {
            Map<String, Object> payload = Map.of(
                "id", UUID.randomUUID().toString(),
                "title", title,
                "message", message,
                "timestamp", Instant.now().toString(),
                "type", "ALARM"
            );
            
            String json = objectMapper.writeValueAsString(payload);
            logWebSocketHandler.broadcastRaw(json);
            
            log.info("Successfully broadcasted in-app notification: {}", title);
        } catch (Exception e) {
            log.error("Failed to broadcast in-app notification", e);
        }
    }
}
