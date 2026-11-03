package com.faultlens.api.ws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.faultlens.api.security.JwtService;
import java.net.URI;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.websocket.servlet.WebSocketServletAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * Real handshakes against an embedded Tomcat with only the WebSocket slice loaded
 * (no database, Kafka or Redis), covering token, sub-protocol negotiation and origin checks.
 */
@SpringBootTest(
        classes = WebSocketHandshakeTest.WebSocketSlice.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "faultlens.jwt.secret=faultlens-test-secret-needs-at-least-256-bits-value",
                "faultlens.jwt.expiration-ms=60000",
                "faultlens.cors.allowed-origins=http://localhost:3000",
                "faultlens.ws.max-connections=10"
        })
class WebSocketHandshakeTest {
    private static final String ALLOWED_ORIGIN = "http://localhost:3000";

    @LocalServerPort
    private int port;

    @Autowired
    private JwtService jwtService;

    @Test
    void connectsWithValidTokenAndNegotiatesBearerProtocol() throws Exception {
        String token = jwtService.generateToken("admin", List.of("ADMIN"));

        WebSocketSession session = connect(ALLOWED_ORIGIN, "bearer", token);
        try {
            assertThat(session.isOpen()).isTrue();
            assertThat(session.getAcceptedProtocol()).isEqualTo("bearer");
        } finally {
            session.close();
        }
    }

    @Test
    void rejectsHandshakeWithoutToken() {
        assertThatThrownBy(() -> connect(ALLOWED_ORIGIN)).isInstanceOf(ExecutionException.class);
    }

    @Test
    void rejectsHandshakeWithInvalidToken() {
        assertThatThrownBy(() -> connect(ALLOWED_ORIGIN, "bearer", "not-a-jwt"))
                .isInstanceOf(ExecutionException.class);
    }

    @Test
    void rejectsHandshakeFromDisallowedOrigin() {
        String token = jwtService.generateToken("admin", List.of("ADMIN"));

        assertThatThrownBy(() -> connect("https://evil.example", "bearer", token))
                .isInstanceOf(ExecutionException.class);
    }

    private WebSocketSession connect(String origin, String... protocols) throws Exception {
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        headers.setOrigin(origin);
        if (protocols.length > 0) {
            headers.setSecWebSocketProtocol(List.of(protocols));
        }
        return new StandardWebSocketClient()
                .execute(new TextWebSocketHandler(), headers, URI.create("ws://localhost:" + port + "/ws/logs"))
                .get(5, TimeUnit.SECONDS);
    }

    @SpringBootConfiguration
    @ImportAutoConfiguration({
            ServletWebServerFactoryAutoConfiguration.class,
            DispatcherServletAutoConfiguration.class,
            WebSocketServletAutoConfiguration.class
    })
    @Import({WebSocketConfig.class, LogWebSocketHandler.class, JwtService.class})
    static class WebSocketSlice {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}
