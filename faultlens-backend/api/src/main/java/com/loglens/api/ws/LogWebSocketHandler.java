package com.loglens.api.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loglens.common.dto.LogEntryDto;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogWebSocketHandler extends TextWebSocketHandler {
    private final ObjectMapper objectMapper;
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    /**
     * Registers a client session.
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
    }

    /**
     * Removes a closed client session.
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
    }

    /**
     * Broadcasts a log entry DTO to all connected clients.
     */
    public void broadcast(LogEntryDto entry) {
        try {
            broadcastRaw(objectMapper.writeValueAsString(entry));
        } catch (Exception ex) {
            log.error("Could not serialize websocket log entry", ex);
        }
    }

    /**
     * Broadcasts a pre-serialized payload to all connected clients.
     */
    public void broadcastRaw(String payload) {
        sessions.removeIf(session -> !session.isOpen());
        for (WebSocketSession session : sessions) {
            try {
                session.sendMessage(new TextMessage(payload));
            } catch (IOException ex) {
                log.warn("WebSocket send failed for session {}", session.getId(), ex);
            }
        }
    }
}
