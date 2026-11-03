package com.faultlens.api.ws;

import com.faultlens.api.security.JwtService;
import io.jsonwebtoken.Claims;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.server.HandshakeInterceptor;

/**
 * Authenticates WebSocket handshakes with a JWT.
 *
 * <p>Browsers cannot set an {@code Authorization} header on a WebSocket request, so the client
 * sends the token as a sub-protocol: {@code new WebSocket(url, ["bearer", token])}. This keeps
 * the token out of URLs (and therefore out of proxy/access logs). The server answers with the
 * {@code bearer} sub-protocol, see {@link LogWebSocketHandler#getSubProtocols()}.
 */
@Slf4j
public class JwtHandshakeInterceptor implements HandshakeInterceptor {
    /** Sub-protocol name that marks the following entry as a bearer token. */
    public static final String BEARER_PROTOCOL = "bearer";
    /** Session attribute holding the authenticated username. */
    public static final String ATTR_USERNAME = "faultlens.username";
    /** Session attribute holding the token expiry in epoch milliseconds. */
    public static final String ATTR_EXPIRES_AT = "faultlens.expiresAt";

    private final JwtService jwtService;

    public JwtHandshakeInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) {
        Optional<String> token = extractToken(request);
        if (token.isEmpty() || !jwtService.validateToken(token.get())) {
            log.debug("WebSocket handshake rejected from {}: missing or invalid token", request.getRemoteAddress());
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        Claims claims = jwtService.parse(token.get());
        attributes.put(ATTR_USERNAME, claims.getSubject());
        if (claims.getExpiration() != null) {
            attributes.put(ATTR_EXPIRES_AT, claims.getExpiration().getTime());
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

    /**
     * Returns the entry that follows {@code bearer} in the {@code Sec-WebSocket-Protocol} header.
     */
    static Optional<String> extractToken(ServerHttpRequest request) {
        List<String> protocols = request.getHeaders().getOrEmpty(WebSocketHttpHeaders.SEC_WEBSOCKET_PROTOCOL).stream()
                .flatMap(value -> Arrays.stream(value.split(",")))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();

        int bearerIndex = protocols.indexOf(BEARER_PROTOCOL);
        if (bearerIndex < 0 || bearerIndex + 1 >= protocols.size()) {
            return Optional.empty();
        }
        return Optional.of(protocols.get(bearerIndex + 1));
    }
}
