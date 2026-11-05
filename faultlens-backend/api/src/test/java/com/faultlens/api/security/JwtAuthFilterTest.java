package com.faultlens.api.security;

import static org.assertj.core.api.Assertions.assertThat;

import tools.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthFilterTest {
    private static final String SECRET = "faultlens-test-secret-needs-at-least-256-bits-value";

    private final JwtService jwtService = new JwtService(SECRET, 60_000);
    private final JwtAuthFilter filter = new JwtAuthFilter(jwtService, new ObjectMapper());

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesRegularToken() throws Exception {
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request("GET", "/api/v1/logs", jwtService.generateToken("admin", List.of("ADMIN"))),
                new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("admin");
    }

    @Test
    void blocksApiAccessWhilePasswordChangeIsPending() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request("GET", "/api/v1/logs", restrictedToken()), response, chain);

        assertThat(chain.getRequest()).isNull();
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("PASSWORD_CHANGE_REQUIRED");
    }

    @Test
    void allowsPasswordChangeEndpointsWhilePasswordChangeIsPending() throws Exception {
        for (String[] endpoint : new String[][] {
                {"PUT", "/api/v1/auth/password"}, {"GET", "/api/v1/auth/profile"}, {"POST", "/api/v1/auth/refresh"}}) {
            MockFilterChain chain = new MockFilterChain();

            filter.doFilter(request(endpoint[0], endpoint[1], restrictedToken()), new MockHttpServletResponse(), chain);

            assertThat(chain.getRequest()).as("%s %s", endpoint[0], endpoint[1]).isNotNull();
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void doesNotAllowProfileUpdateWhilePasswordChangeIsPending() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request("PUT", "/api/v1/auth/profile", restrictedToken()), response, chain);

        assertThat(chain.getRequest()).isNull();
        assertThat(response.getStatus()).isEqualTo(403);
    }

    private String restrictedToken() {
        return jwtService.generateToken("admin", List.of("ADMIN"), true);
    }

    private static MockHttpServletRequest request(String method, String uri, String token) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }
}
