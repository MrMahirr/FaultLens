package com.loglens.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.loglens.api.dto.AuthDtos.LoginRequest;
import com.loglens.api.model.UserAccount;
import com.loglens.api.repository.UserAccountRepository;
import com.loglens.api.security.JwtService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceTest {
    private static final String SECRET = "loglens-test-secret-needs-at-least-256-bits-value";

    @Test
    void authenticatesUserAndReturnsToken() {
        UserAccountRepository repository = mock(UserAccountRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        JwtService jwtService = new JwtService(SECRET, 60_000);
        UserAccount user = UserAccount.builder()
                .username("admin")
                .password("hash")
                .role("ADMIN")
                .build();
        when(repository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "hash")).thenReturn(true);

        AuthService service = new AuthService(repository, passwordEncoder, jwtService);

        var response = service.login(new LoginRequest("admin", "secret"));

        assertThat(jwtService.validateToken(response.token())).isTrue();
        assertThat(jwtService.extractUsername(response.token())).isEqualTo("admin");
        assertThat(response.expiresAt()).isNotNull();
    }

    @Test
    void rejectsInvalidPassword() {
        UserAccountRepository repository = mock(UserAccountRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        JwtService jwtService = new JwtService(SECRET, 60_000);
        UserAccount user = UserAccount.builder()
                .username("admin")
                .password("hash")
                .role("ADMIN")
                .build();
        when(repository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        AuthService service = new AuthService(repository, passwordEncoder, jwtService);

        assertThatThrownBy(() -> service.login(new LoginRequest("admin", "wrong")))
                .isInstanceOf(BadCredentialsException.class);
    }
}
