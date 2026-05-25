package com.loglens.api.ws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;

class WebSocketIntegrationTest {
    private LogWebSocketHandler handler;
    private WebSocketConfig config;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        handler = new LogWebSocketHandler(objectMapper, 2); // Limit set to 2 for easy testing
        config = new WebSocketConfig(handler, 2);
    }

    @Test
    void connectionEstablishmentDecoratesAndStoresSession() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("sess-1");
        when(session.isOpen()).thenReturn(true);

        handler.afterConnectionEstablished(session);

        assertThat(handler.getActiveConnectionCount()).isEqualTo(1);
    }

    @Test
    void connectionRejectsBeyondMaxConnections() throws Exception {
        WebSocketSession session1 = mock(WebSocketSession.class);
        when(session1.getId()).thenReturn("sess-1");
        when(session1.isOpen()).thenReturn(true);

        WebSocketSession session2 = mock(WebSocketSession.class);
        when(session2.getId()).thenReturn("sess-2");
        when(session2.isOpen()).thenReturn(true);

        WebSocketSession session3 = mock(WebSocketSession.class);
        when(session3.getId()).thenReturn("sess-3");
        when(session3.isOpen()).thenReturn(true);

        handler.afterConnectionEstablished(session1);
        handler.afterConnectionEstablished(session2);
        
        // This third one should be rejected immediately
        handler.afterConnectionEstablished(session3);

        verify(session3).close(eq(CloseStatus.POLICY_VIOLATION.withReason("Max connection limit reached")));
        assertThat(handler.getActiveConnectionCount()).isEqualTo(2);
    }

    @Test
    void afterConnectionClosedRemovesSession() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("sess-1");
        when(session.isOpen()).thenReturn(true);

        handler.afterConnectionEstablished(session);
        assertThat(handler.getActiveConnectionCount()).isEqualTo(1);

        handler.afterConnectionClosed(session, CloseStatus.NORMAL);
        assertThat(handler.getActiveConnectionCount()).isEqualTo(0);
    }

    @Test
    void handshakeInterceptorRejectsWhenLimitReached() throws Exception {
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        ServerHttpResponse response = mock(ServerHttpResponse.class);
        WebSocketHandler wsHandler = mock(WebSocketHandler.class);
        Map<String, Object> attributes = new HashMap<>();

        WebSocketConfig.HandshakeLimitInterceptor interceptor = config.new HandshakeLimitInterceptor();

        // 1. Connection count is 0, should allow
        boolean allowed = interceptor.beforeHandshake(request, response, wsHandler, attributes);
        assertThat(allowed).isTrue();

        // 2. Add 2 connections to reach limit
        WebSocketSession session1 = mock(WebSocketSession.class);
        when(session1.getId()).thenReturn("sess-1");
        when(session1.isOpen()).thenReturn(true);
        WebSocketSession session2 = mock(WebSocketSession.class);
        when(session2.getId()).thenReturn("sess-2");
        when(session2.isOpen()).thenReturn(true);

        handler.afterConnectionEstablished(session1);
        handler.afterConnectionEstablished(session2);

        // 3. Connection count is at limit (2), should reject
        boolean blocked = interceptor.beforeHandshake(request, response, wsHandler, attributes);
        assertThat(blocked).isFalse();
        verify(response).setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
    }
}
