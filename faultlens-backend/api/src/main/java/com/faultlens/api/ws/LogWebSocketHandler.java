package com.faultlens.api.ws;

import tools.jackson.databind.ObjectMapper;
import com.faultlens.common.dto.LogEntryDto;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.SubProtocolCapable;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Slf4j
@Component
public class LogWebSocketHandler extends TextWebSocketHandler implements SubProtocolCapable, DisposableBean {
    /** Application close code sent when the session's JWT has expired (4000-4999 = app-defined). */
    static final CloseStatus TOKEN_EXPIRED = new CloseStatus(4401, "Token expired");

    private final ObjectMapper objectMapper;
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    
    private final int maxConnections;
    private final ExecutorService broadcastExecutor;
    private final ScheduledExecutorService heartbeatScheduler;

    public LogWebSocketHandler(
            ObjectMapper objectMapper,
            @Value("${faultlens.ws.max-connections:1000}") int maxConnections) {
        this.objectMapper = objectMapper;
        this.maxConnections = maxConnections;
        
        // Bounded thread pool to prevent OOM
        this.broadcastExecutor = new ThreadPoolExecutor(
                4, // Core pool size
                16, // Max pool size
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(5000), // Bounded capacity
                new ThreadPoolExecutor.DiscardOldestPolicy() // Protect from OOM by discarding oldest
        );

        this.heartbeatScheduler = new ScheduledThreadPoolExecutor(1);
        this.heartbeatScheduler.scheduleAtFixedRate(this::sendHeartbeats, 10, 10, TimeUnit.SECONDS);
    }

    /**
     * Accepts the {@code bearer} sub-protocol offered by authenticated clients
     * ({@code ["bearer", token]}); see {@link JwtHandshakeInterceptor}.
     */
    @Override
    public List<String> getSubProtocols() {
        return List.of(JwtHandshakeInterceptor.BEARER_PROTOCOL);
    }

    /**
     * Gets the active connection count.
     */
    public int getActiveConnectionCount() {
        return sessions.size();
    }

    /**
     * Registers a client session.
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        if (sessions.size() >= maxConnections) {
            log.warn("Max connection limit reached ({}). Rejecting session {}", maxConnections, session.getId());
            session.close(CloseStatus.POLICY_VIOLATION.withReason("Max connection limit reached"));
            return;
        }

        // Decorate session to handle slow consumers and enable concurrent/async writes safely
        WebSocketSession decoratedSession = new ConcurrentWebSocketSessionDecorator(session, 5000, 1024 * 1024);
        sessions.put(session.getId(), decoratedSession);
        log.info("WebSocket connection established: {}. Active sessions: {}", session.getId(), sessions.size());
    }

    /**
     * Removes a closed client session.
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
        log.info("WebSocket connection closed: {}. Reason: {}. Active sessions: {}", session.getId(), status, sessions.size());
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
     * Broadcasts a pre-serialized payload asynchronously to all connected clients.
     */
    public void broadcastRaw(String payload) {
        if (sessions.isEmpty()) {
            return;
        }

        broadcastExecutor.submit(() -> {
            sessions.values().removeIf(session -> !session.isOpen());
            for (WebSocketSession session : sessions.values()) {
                try {
                    session.sendMessage(new TextMessage(payload));
                } catch (Exception ex) {
                    log.warn("WebSocket async send failed for session {}: {}", session.getId(), ex.getMessage());
                }
            }
        });
    }

    /**
     * Periodically pings all sessions to clean up dead connections and closes sessions whose
     * token has expired, so a revoked/expired login cannot keep streaming logs.
     */
    void sendHeartbeats() {
        if (sessions.isEmpty()) {
            return;
        }
        
        sessions.values().removeIf(session -> !session.isOpen());
        
        long now = System.currentTimeMillis();
        for (WebSocketSession session : sessions.values()) {
            if (isExpired(session, now)) {
                log.info("Closing WebSocket session {}: token expired", session.getId());
                sessions.remove(session.getId());
                try {
                    session.close(TOKEN_EXPIRED);
                } catch (IOException ioEx) {
                    // Ignore
                }
                continue;
            }
            try {
                session.sendMessage(new PingMessage(ByteBuffer.wrap(new byte[0])));
            } catch (Exception ex) {
                log.warn("Ping failed for session {}, closing connection: {}", session.getId(), ex.getMessage());
                try {
                    session.close(CloseStatus.SESSION_NOT_RELIABLE);
                } catch (IOException ioEx) {
                    // Ignore
                }
            }
        }
    }

    private static boolean isExpired(WebSocketSession session, long now) {
        Object expiresAt = session.getAttributes().get(JwtHandshakeInterceptor.ATTR_EXPIRES_AT);
        return expiresAt instanceof Long expiry && expiry <= now;
    }

    @Override
    public void destroy() {
        log.info("Shutting down WebSocket executors and closing active sessions...");
        heartbeatScheduler.shutdownNow();
        broadcastExecutor.shutdown();
        try {
            if (!broadcastExecutor.awaitTermination(3, TimeUnit.SECONDS)) {
                broadcastExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            broadcastExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        for (WebSocketSession session : sessions.values()) {
            try {
                session.close(CloseStatus.GOING_AWAY);
            } catch (IOException e) {
                // Ignore
            }
        }
        sessions.clear();
    }
}

