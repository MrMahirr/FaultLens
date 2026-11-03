package com.faultlens.api.ws;

import com.faultlens.api.security.JwtService;
import java.util.Arrays;
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
    private final JwtService jwtService;
    private final int maxConnections;
    private final String[] allowedOrigins;

    public WebSocketConfig(
            LogWebSocketHandler handler,
            JwtService jwtService,
            @Value("${faultlens.ws.max-connections:1000}") int maxConnections,
            @Value("${faultlens.cors.allowed-origins:http://localhost:3000}") String allowedOrigins) {
        this.handler = handler;
        this.jwtService = jwtService;
        this.maxConnections = maxConnections;
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toArray(String[]::new);
    }

    /**
     * Registers the raw WebSocket endpoint.
     *
     * <p>Origins are limited to the same list as REST CORS ({@code CORS_ORIGINS}). Interceptor order
     * matters: unauthenticated clients are rejected before the connection limit is consulted.
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/logs")
                .setAllowedOrigins(allowedOrigins)
                .addInterceptors(new JwtHandshakeInterceptor(jwtService), new HandshakeLimitInterceptor());
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
