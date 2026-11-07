package com.faultlens.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.faultlens.api.service.ApiKeyService;
import com.faultlens.api.service.ApiKeyService.VerifiedKey;
import java.util.EnumSet;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.ObjectMapper;

class ApiKeyAuthFilterTest {
    private static final String TOKEN = ApiKeyTokens.generate().token();

    private final ApiKeyService service = mock(ApiKeyService.class);
    private final ApiKeyRateLimiter rateLimiter = mock(ApiKeyRateLimiter.class);
    private final ApiKeyAuthFilter filter = new ApiKeyAuthFilter(service, rateLimiter, new ObjectMapper());

    @BeforeEach
    void allowByDefault() {
        when(rateLimiter.tryAcquire(any())).thenReturn(true);
        when(service.verify(TOKEN)).thenReturn(Optional.of(
                new VerifiedKey(7L, "ci", EnumSet.of(ApiKeyScope.DEPLOYMENTS_WRITE))));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void passesRequestsWithoutAKeyThrough() throws Exception {
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/logs");
        request.addHeader("Authorization", "Bearer eyJhbGciOiJIUzI1NiJ9.x.y");

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(service, never()).verify(anyString());
    }

    @Test
    void authenticatesAKeyWithinItsScopeWithScopeAuthoritiesOnly() throws Exception {
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(bearer("POST", "/api/v1/deployments"), new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isNotNull();
        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth.getName()).isEqualTo("api-key:ci");
        assertThat(auth.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("SCOPE_deployments:write");
        verify(service).recordUse(7L, "127.0.0.1");
    }

    @Test
    void acceptsTheDedicatedHeader() throws Exception {
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/deployments");
        request.addHeader(ApiKeyAuthFilter.HEADER, TOKEN);

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void refusesRequestsOutsideTheScope() throws Exception {
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(bearer("GET", "/api/v1/logs"), response, chain);

        assertThat(chain.getRequest()).isNull();
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("API_KEY_SCOPE");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void unknownKeyIsAHard401() throws Exception {
        String other = ApiKeyTokens.generate().token();
        when(service.verify(other)).thenReturn(Optional.empty());
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/deployments");
        request.addHeader("Authorization", "Bearer " + other);
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNull();
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("INVALID_API_KEY");
    }

    @Test
    void rateLimitedKeyGets429WithRetryAfter() throws Exception {
        when(rateLimiter.tryAcquire(7L)).thenReturn(false);
        when(rateLimiter.retryAfterSeconds()).thenReturn(12L);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(bearer("POST", "/api/v1/deployments"), response, chain);

        assertThat(chain.getRequest()).isNull();
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isEqualTo("12");
    }

    private static MockHttpServletRequest bearer(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.addHeader("Authorization", "Bearer " + TOKEN);
        return request;
    }
}
