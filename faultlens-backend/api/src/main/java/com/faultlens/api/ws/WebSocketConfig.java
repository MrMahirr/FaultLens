package com.faultlens.api.ws;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;

@Slf4j
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final LogWebSocketHandler handler;
    private final int maxConnections;

    public WebSocketConfig(
            LogWebSocketHandler handler,
            @Value("${faultlens.ws.max-connections:1000}") int maxConnections) {
        this.handler = handler;
        this.maxConnections = maxConnections;
    }

    /**
     * Registers the raw WebSocket endpoint.
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/logs")
                .setAllowedOriginPatterns("*")
                .addInterceptors(new HandshakeLimitInterceptor());
    }

    /**
     * Handshake interceptor to reject connections when limit is reached.
     */
    class HandshakeLimitInterceptor implements HandshakeInterceptor {
        @Override
        public boolean beforeHandshake(
                ServerHttpRequest request,
                ServerHttpResponse response,
                WebSocketHandler wsHandler,
                Map<String, Object> attributes) throws Exception {
            
            int activeConnections = handler.getActiveConnectionCount();
            if (activeConnections >= maxConnections) {
                log.warn("WebSocket handshake rejected. Max connections reached (limit: {}, active: {})", maxConnections, activeConnections);
                response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
                return false;
            }
            return true;
        }

        @Override
        public void afterHandshake(
                ServerHttpRequest request,
                ServerHttpResponse response,
                WebSocketHandler wsHandler,
                Exception exception) {
            // No-op
        }
    }
}

