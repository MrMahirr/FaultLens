package com.faultlens.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.faultlens.api.dto.AuthDtos.ChangePasswordRequest;
import com.faultlens.api.dto.AuthDtos.LoginRequest;
import com.faultlens.api.dto.AuthDtos.UpdateProfileRequest;
import com.faultlens.api.model.UserAccount;
import com.faultlens.api.repository.UserAccountRepository;
import com.faultlens.api.security.JwtService;
import com.faultlens.common.exception.ConflictException;
import com.faultlens.common.exception.FaultLensException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceTest {
    private static final String SECRET = "faultlens-test-secret-needs-at-least-256-bits-value";

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

    private static final String V6_SEED_HASH = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder(4);

    @Test
    void seededAdminBackdoorNoLongerWorks() {
        UserAccountRepository repository = mock(UserAccountRepository.class);
        UserAccount admin = UserAccount.builder().username("admin").password(V6_SEED_HASH).role("ADMIN").build();
        when(repository.findByUsername("admin")).thenReturn(Optional.of(admin));
        AuthService service = new AuthService(repository, bcrypt, new JwtService(SECRET, 60_000));

        assertThatThrownBy(() -> service.login(new LoginRequest("admin", "admin123")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void loginFlagsPendingPasswordChangeInResponseAndToken() {
        UserAccountRepository repository = mock(UserAccountRepository.class);
        JwtService jwtService = new JwtService(SECRET, 60_000);
        UserAccount admin = UserAccount.builder()
                .username("admin").password(bcrypt.encode("Initial-Pass-1")).role("ADMIN")
                .mustChangePassword(true).build();
        when(repository.findByUsername("admin")).thenReturn(Optional.of(admin));
        AuthService service = new AuthService(repository, bcrypt, jwtService);

        var response = service.login(new LoginRequest("admin", "Initial-Pass-1"));

        assertThat(response.mustChangePassword()).isTrue();
        assertThat(JwtService.requiresPasswordChange(jwtService.parse(response.token()))).isTrue();
    }

    @Test
    void changePasswordClearsFlagAndReturnsUnrestrictedToken() {
        UserAccountRepository repository = mock(UserAccountRepository.class);
        JwtService jwtService = new JwtService(SECRET, 60_000);
        UserAccount admin = UserAccount.builder()
                .username("admin").password(bcrypt.encode("Initial-Pass-1")).role("ADMIN")
                .mustChangePassword(true).build();
        when(repository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(repository.save(any(UserAccount.class))).thenAnswer(inv -> inv.getArgument(0));
        AuthService service = new AuthService(repository, bcrypt, jwtService);

        var response = service.changePassword("admin", new ChangePasswordRequest("Initial-Pass-1", "Brand-New-Pass-2"));

        assertThat(admin.isMustChangePassword()).isFalse();
        assertThat(bcrypt.matches("Brand-New-Pass-2", admin.getPassword())).isTrue();
        assertThat(response.mustChangePassword()).isFalse();
        assertThat(JwtService.requiresPasswordChange(jwtService.parse(response.token()))).isFalse();
    }

    @Test
    void changePasswordWithWrongCurrentPasswordIsNotAnAuthFailure() {
        UserAccountRepository repository = mock(UserAccountRepository.class);
        UserAccount admin = UserAccount.builder()
                .username("admin").password(bcrypt.encode("Initial-Pass-1")).role("ADMIN").build();
        when(repository.findByUsername("admin")).thenReturn(Optional.of(admin));
        AuthService service = new AuthService(repository, bcrypt, new JwtService(SECRET, 60_000));

        // 400 (FaultLensException), not 401: a 401 would log the user out in the frontend
        assertThatThrownBy(() -> service.changePassword("admin", new ChangePasswordRequest("wrong", "Brand-New-Pass-2")))
                .isInstanceOf(FaultLensException.class)
                .hasFieldOrPropertyWithValue("errorCode", "INVALID_CURRENT_PASSWORD");
    }

    @Test
    void changePasswordRejectsReusingCurrentPassword() {
        UserAccountRepository repository = mock(UserAccountRepository.class);
        UserAccount admin = UserAccount.builder()
                .username("admin").password(bcrypt.encode("Initial-Pass-1")).role("ADMIN").build();
        when(repository.findByUsername("admin")).thenReturn(Optional.of(admin));
        AuthService service = new AuthService(repository, bcrypt, new JwtService(SECRET, 60_000));

        assertThatThrownBy(() -> service.changePassword("admin", new ChangePasswordRequest("Initial-Pass-1", "Initial-Pass-1")))
                .isInstanceOf(FaultLensException.class)
                .hasFieldOrPropertyWithValue("errorCode", "PASSWORD_REUSED");
    }

    // ── Profile update (P06): a rename must not leave the caller with a token for a missing user ──

    @Test
    void renameReturnsATokenForTheNewUsername() {
        UserAccountRepository repository = mock(UserAccountRepository.class);
        JwtService jwtService = new JwtService(SECRET, 60_000);
        UserAccount user = UserAccount.builder().username("admin").password("hash").role("ADMIN").build();
        when(repository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(repository.findByUsername("mahir")).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(UserAccount.class))).thenAnswer(inv -> inv.getArgument(0));
        AuthService service = new AuthService(repository, mock(PasswordEncoder.class), jwtService);

        var response = service.updateProfile("admin", new UpdateProfileRequest("  mahir ", "m@example.com"));

        assertThat(response.username()).isEqualTo("mahir");
        assertThat(response.email()).isEqualTo("m@example.com");
        assertThat(jwtService.extractUsername(response.token())).isEqualTo("mahir");
        assertThat(response.expiresAt()).isNotNull();
    }

    @Test
    void renamingToATakenUsernameIsAConflict() {
        UserAccountRepository repository = mock(UserAccountRepository.class);
        UserAccount user = UserAccount.builder().username("admin").password("hash").role("ADMIN").build();
        UserAccount other = UserAccount.builder().username("ops").password("hash").role("ADMIN").build();
        when(repository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(repository.findByUsername("ops")).thenReturn(Optional.of(other));
        AuthService service = new AuthService(repository, mock(PasswordEncoder.class), new JwtService(SECRET, 60_000));

        assertThatThrownBy(() -> service.updateProfile("admin", new UpdateProfileRequest("ops", null)))
                .isInstanceOf(ConflictException.class)
                .hasFieldOrPropertyWithValue("errorCode", "USERNAME_TAKEN");
    }

    @Test
    void concurrentlyTakenUsernameIsAConflictToo() {
        UserAccountRepository repository = mock(UserAccountRepository.class);
        UserAccount user = UserAccount.builder().username("admin").password("hash").role("ADMIN").build();
        when(repository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(repository.findByUsername("ops")).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(UserAccount.class)))
                .thenThrow(new DataIntegrityViolationException("users_username_key"));
        AuthService service = new AuthService(repository, mock(PasswordEncoder.class), new JwtService(SECRET, 60_000));

        assertThatThrownBy(() -> service.updateProfile("admin", new UpdateProfileRequest("ops", null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void keepingTheUsernameStillReturnsAWorkingToken() {
        UserAccountRepository repository = mock(UserAccountRepository.class);
        JwtService jwtService = new JwtService(SECRET, 60_000);
        UserAccount user = UserAccount.builder().username("admin").password("hash").role("ADMIN").build();
        when(repository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(repository.saveAndFlush(any(UserAccount.class))).thenAnswer(inv -> inv.getArgument(0));
        AuthService service = new AuthService(repository, mock(PasswordEncoder.class), jwtService);

        var response = service.updateProfile("admin", new UpdateProfileRequest("admin", "new@example.com"));

        assertThat(jwtService.extractUsername(response.token())).isEqualTo("admin");
        assertThat(response.email()).isEqualTo("new@example.com");
    }
}
