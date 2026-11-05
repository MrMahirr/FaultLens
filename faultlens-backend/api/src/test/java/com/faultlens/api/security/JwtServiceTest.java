package com.faultlens.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class JwtServiceTest {
    private static final String SECRET = "faultlens-test-secret-needs-at-least-256-bits-value";

    @Test
    void generatesValidTokenAndExtractsUsername() {
        JwtService jwtService = new JwtService(SECRET, 60_000);

        String token = jwtService.generateToken("admin", List.of("ADMIN"));

        assertThat(jwtService.validateToken(token)).isTrue();
        assertThat(jwtService.extractUsername(token)).isEqualTo("admin");
        assertThat(jwtService.parse(token).get("roles", List.class)).contains("ADMIN");
    }

    @Test
    void rejectsInvalidToken() {
        JwtService jwtService = new JwtService(SECRET, 60_000);

        assertThat(jwtService.validateToken("invalid-token")).isFalse();
    }

    @Test
    void refusesToStartWithThePublishedExampleSecret() {
        assertThatThrownBy(() -> new JwtService("faultlens-secret-change-in-production-needs-256-bits-minimum", 60_000))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("example value");
    }

    @Test
    void refusesToStartWithShortSecret() {
        assertThatThrownBy(() -> new JwtService("too-short-secret", 60_000))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 bytes");
    }

    @Test
    void generatesRandomKeyWhenSecretIsNotConfigured() {
        JwtService first = new JwtService("", 60_000);
        JwtService second = new JwtService(null, 60_000);

        String token = first.generateToken("admin", List.of("ADMIN"));

        assertThat(first.validateToken(token)).isTrue();
        // random per instance: nobody (including another instance) can verify or forge it
        assertThat(second.validateToken(token)).isFalse();
    }
}
