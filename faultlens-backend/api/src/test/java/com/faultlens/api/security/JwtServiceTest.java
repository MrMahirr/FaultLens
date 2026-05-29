package com.faultlens.api.security;

import static org.assertj.core.api.Assertions.assertThat;

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
}
