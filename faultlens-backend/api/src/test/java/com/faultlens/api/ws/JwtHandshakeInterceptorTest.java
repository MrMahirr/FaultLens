package com.faultlens.api.ws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.faultlens.api.security.JwtService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.socket.WebSocketHandler;

class JwtHandshakeInterceptorTest {
    private static final String SECRET = "faultlens-test-secret-needs-at-least-256-bits-value";

    private final JwtService jwtService = new JwtService(SECRET, 60_000);
    private final JwtHandshakeInterceptor interceptor = new JwtHandshakeInterceptor(jwtService);

    @Test
    void acceptsValidBearerTokenAndStoresSessionAttributes() {
        String token = jwtService.generateToken("admin", List.of("ADMIN"));
        Map<String, Object> attributes = new HashMap<>();
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();

        boolean allowed = interceptor.beforeHandshake(
                request("bearer, " + token), new ServletServerHttpResponse(servletResponse),
                mock(WebSocketHandler.class), attributes);

        assertThat(allowed).isTrue();
        assertThat(attributes).containsEntry(JwtHandshakeInterceptor.ATTR_USERNAME, "admin");
        assertThat((Long) attributes.get(JwtHandshakeInterceptor.ATTR_EXPIRES_AT))
                .isGreaterThan(System.currentTimeMillis());
    }

    @Test
    void rejectsHandshakeWithoutToken() {
        assertRejected(request(null));
    }

    @Test
    void rejectsBearerProtocolWithoutTokenValue() {
        assertRejected(request("bearer"));
    }

    @Test
    void rejectsInvalidToken() {
        assertRejected(request("bearer, not-a-jwt"));
    }

    @Test
    void rejectsTokenSignedWithAnotherSecret() {
        JwtService other = new JwtService("another-secret-that-is-also-at-least-256-bits-long", 60_000);
        assertRejected(request("bearer, " + other.generateToken("admin", List.of("ADMIN"))));
    }

    @Test
    void rejectsExpiredToken() {
        JwtService shortLived = new JwtService(SECRET, -1_000);
        assertRejected(request("bearer, " + shortLived.generateToken("admin", List.of("ADMIN"))));
    }

    @Test
    void rejectsTokenWithPendingPasswordChange() {
        String token = jwtService.generateToken("admin", List.of("ADMIN"), true);
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();

        boolean allowed = interceptor.beforeHandshake(
                request("bearer, " + token), new ServletServerHttpResponse(servletResponse),
                mock(WebSocketHandler.class), new HashMap<>());

        assertThat(allowed).isFalse();
        assertThat(servletResponse.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void extractsTokenRegardlessOfProtocolOrder() {
        assertThat(JwtHandshakeInterceptor.extractToken(request("v1.logs, bearer, abc.def.ghi")))
                .contains("abc.def.ghi");
    }

    private void assertRejected(ServletServerHttpRequest request) {
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        Map<String, Object> attributes = new HashMap<>();

        boolean allowed = interceptor.beforeHandshake(
                request, new ServletServerHttpResponse(servletResponse), mock(WebSocketHandler.class), attributes);

        assertThat(allowed).isFalse();
        assertThat(servletResponse.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(attributes).isEmpty();
    }

    private static ServletServerHttpRequest request(String protocolHeader) {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/ws/logs");
        if (protocolHeader != null) {
            servletRequest.addHeader("Sec-WebSocket-Protocol", protocolHeader);
        }
        return new ServletServerHttpRequest(servletRequest);
    }
}
