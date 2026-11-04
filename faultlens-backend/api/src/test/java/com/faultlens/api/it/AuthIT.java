package com.faultlens.api.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.faultlens.api.security.JwtService;
import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * Locks the security filter chain end to end (P05's Spring Security 7 upgrade rewrites it):
 * JWT login, the forced first-login password change, protected endpoints and the WebSocket
 * handshake (token as {@code bearer} sub-protocol + origin check).
 */
class AuthIT extends AbstractIntegrationTest {
    private static final String ALLOWED_ORIGIN = "http://localhost:3000";

    @Autowired
    private JwtService jwtService;

    @Test
    void loginWithWrongPasswordIsRejected() {
        ResponseEntity<JsonNode> response = login("admin", "definitely-wrong");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().at("/success").asBoolean(true)).isFalse();
    }

    @Test
    void protectedEndpointRequiresToken() {
        assertThat(get("/api/v1/alarms", null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(get("/api/v1/alarms", "not-a-jwt").getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void validTokenOpensProtectedEndpoints() {
        String token = adminToken();

        assertThat(get("/api/v1/alarms", token).getStatusCode()).isEqualTo(HttpStatus.OK);
        ResponseEntity<JsonNode> profile = get("/api/v1/auth/profile", token);
        assertThat(profile.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(profile.getBody().at("/data/username").asText()).isEqualTo("admin");
    }

    @Test
    void refreshReturnsAWorkingToken() {
        ResponseEntity<JsonNode> refreshed = exchange(HttpMethod.POST, "/api/v1/auth/refresh", null,
                java.util.Map.of("token", adminToken()));

        assertThat(refreshed.getStatusCode()).isEqualTo(HttpStatus.OK);
        String token = refreshed.getBody().at("/data/token").asText();
        assertThat(get("/api/v1/alarms", token).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void passwordChangeTokenOnlyReachesTheAllowedEndpoints() {
        String restricted = jwtService.generateToken("admin", List.of("ADMIN"), true);

        ResponseEntity<JsonNode> blocked = get("/api/v1/alarms", restricted);
        assertThat(blocked.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(blocked.getBody().at("/error/code").asText()).isEqualTo("PASSWORD_CHANGE_REQUIRED");

        assertThat(get("/api/v1/auth/profile", restricted).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void actuatorHealthIsPublicButMetricsNeedAdmin() {
        assertThat(get("/actuator/health", null).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(get("/actuator/metrics", null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(get("/actuator/metrics", adminToken()).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void webSocketAcceptsValidTokenFromAllowedOrigin() throws Exception {
        WebSocketSession session = connect(ALLOWED_ORIGIN, "bearer", adminToken());
        try {
            assertThat(session.isOpen()).isTrue();
            assertThat(session.getAcceptedProtocol()).isEqualTo("bearer");
        } finally {
            session.close();
        }
    }

    @Test
    void webSocketRejectsMissingInvalidOrRestrictedTokens() {
        String restricted = jwtService.generateToken("admin", List.of("ADMIN"), true);

        assertThatThrownBy(() -> connect(ALLOWED_ORIGIN)).isInstanceOf(Exception.class);
        assertThatThrownBy(() -> connect(ALLOWED_ORIGIN, "bearer", "not-a-jwt")).isInstanceOf(Exception.class);
        assertThatThrownBy(() -> connect(ALLOWED_ORIGIN, "bearer", restricted)).isInstanceOf(Exception.class);
    }

    @Test
    void webSocketRejectsForeignOrigin() {
        assertThatThrownBy(() -> connect("http://evil.example", "bearer", adminToken())).isInstanceOf(Exception.class);
    }

    private WebSocketSession connect(String origin, String... protocols) throws Exception {
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        headers.setOrigin(origin);
        if (protocols.length > 0) {
            headers.setSecWebSocketProtocol(List.of(protocols));
        }
        return new StandardWebSocketClient()
                .execute(new TextWebSocketHandler(), headers, URI.create("ws://localhost:" + port + "/ws/logs"))
                .get(10, TimeUnit.SECONDS);
    }
}
