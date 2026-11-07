package com.faultlens.api.security;

import com.faultlens.api.dto.ErrorEnvelope;
import com.faultlens.api.service.ApiKeyService;
import com.faultlens.api.service.ApiKeyService.VerifiedKey;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

/**
 * Authenticates API keys (P10 / 1.1), sent as {@code Authorization: Bearer flk_…} or
 * {@code X-FaultLens-Key: flk_…}. Runs before {@link JwtAuthFilter}.
 *
 * <p>Unlike a user token a key is confined to its scopes: a request outside them is refused here
 * (403), before Spring Security looks at the URL rules. An invalid, revoked or expired key is a
 * hard 401 rather than "anonymous", so a CI job notices at once. The principal name is
 * {@code api-key:<name>}; authorities are {@code SCOPE_<scope>} only (no role).
 */
@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {
    public static final String HEADER = "X-FaultLens-Key";
    public static final String PRINCIPAL_PREFIX = "api-key:";

    private final ApiKeyService apiKeys;
    private final ApiKeyRateLimiter rateLimiter;
    private final ObjectMapper objectMapper;

    public ApiKeyAuthFilter(ApiKeyService apiKeys, ApiKeyRateLimiter rateLimiter, ObjectMapper objectMapper) {
        this.apiKeys = apiKeys;
        this.rateLimiter = rateLimiter;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Optional<String> token = presentedKey(request);
        if (token.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<VerifiedKey> verified = apiKeys.verify(token.get());
        if (verified.isEmpty()) {
            reject(response, HttpServletResponse.SC_UNAUTHORIZED, "INVALID_API_KEY",
                    "The API key is unknown, revoked or expired");
            return;
        }
        VerifiedKey key = verified.get();

        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (key.scopes().stream().noneMatch(scope -> scope.allows(request.getMethod(), path))) {
            reject(response, HttpServletResponse.SC_FORBIDDEN, "API_KEY_SCOPE",
                    "This API key may not call " + request.getMethod() + " " + path);
            return;
        }

        if (!rateLimiter.tryAcquire(key.id())) {
            response.setHeader("Retry-After", Long.toString(rateLimiter.retryAfterSeconds()));
            reject(response, 429, "RATE_LIMITED", "Too many requests for this API key");
            return;
        }

        List<SimpleGrantedAuthority> authorities = key.scopes().stream()
                .map(ApiKeyScope::authority).map(SimpleGrantedAuthority::new).toList();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(PRINCIPAL_PREFIX + key.name(), null, authorities));
        apiKeys.recordUse(key.id(), request.getRemoteAddr());
        filterChain.doFilter(request, response);
    }

    /** The key from the dedicated header, or from a bearer token that has the key format. */
    static Optional<String> presentedKey(HttpServletRequest request) {
        String header = request.getHeader(HEADER);
        if (header != null && !header.isBlank()) {
            return Optional.of(header.trim());
        }
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String bearer = authorization.substring(7).trim();
            if (ApiKeyTokens.looksLikeKey(bearer)) {
                return Optional.of(bearer);
            }
        }
        return Optional.empty();
    }

    private void reject(HttpServletResponse response, int status, String code, String message) throws IOException {
        SecurityContextHolder.clearContext();
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ErrorEnvelope.of(code, message));
    }
}
