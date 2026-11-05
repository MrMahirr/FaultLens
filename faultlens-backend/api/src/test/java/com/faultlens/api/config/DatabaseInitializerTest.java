package com.faultlens.api.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.faultlens.api.model.UserAccount;
import com.faultlens.api.repository.UserAccountRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class DatabaseInitializerTest {
    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder(4);
    private final UserAccountRepository repository = mock(UserAccountRepository.class);

    @Test
    void createsAdminFromEnvPasswordAndRequiresChange() {
        when(repository.findByUsername("admin")).thenReturn(Optional.empty());

        new DatabaseInitializer(repository, bcrypt, "Env-Provided-Pass-1").run();

        UserAccount saved = captureSaved();
        assertThat(saved.getRole()).isEqualTo("ADMIN");
        assertThat(saved.isMustChangePassword()).isTrue();
        assertThat(bcrypt.matches("Env-Provided-Pass-1", saved.getPassword())).isTrue();
    }

    @Test
    void createsAdminWithGeneratedPasswordWhenEnvIsEmpty() {
        when(repository.findByUsername("admin")).thenReturn(Optional.empty());

        new DatabaseInitializer(repository, bcrypt, "").run();

        UserAccount saved = captureSaved();
        assertThat(saved.isMustChangePassword()).isTrue();
        assertThat(DatabaseInitializer.KNOWN_DEFAULT_PASSWORDS)
                .noneMatch(candidate -> bcrypt.matches(candidate, saved.getPassword()));
    }

    @Test
    void resetsAdminStillUsingV6SeedHash() {
        UserAccount admin = admin(DatabaseInitializer.V6_SEED_HASH);
        when(repository.findByUsername("admin")).thenReturn(Optional.of(admin));

        new DatabaseInitializer(repository, bcrypt, "Env-Provided-Pass-1").run();

        assertThat(admin.getPassword()).isNotEqualTo(DatabaseInitializer.V6_SEED_HASH);
        assertThat(bcrypt.matches("Env-Provided-Pass-1", admin.getPassword())).isTrue();
        assertThat(admin.isMustChangePassword()).isTrue();
        verify(repository).save(admin);
    }

    @Test
    void resetsAdminCreatedWithOldAdmin123Default() {
        UserAccount admin = admin(bcrypt.encode("admin123"));
        when(repository.findByUsername("admin")).thenReturn(Optional.of(admin));

        new DatabaseInitializer(repository, bcrypt, "").run();

        assertThat(bcrypt.matches("admin123", admin.getPassword())).isFalse();
        assertThat(admin.isMustChangePassword()).isTrue();
    }

    @Test
    void leavesAdminWithCustomPasswordUntouched() {
        String customHash = bcrypt.encode("My-Own-Strong-Pass-9");
        UserAccount admin = admin(customHash);
        when(repository.findByUsername("admin")).thenReturn(Optional.of(admin));

        new DatabaseInitializer(repository, bcrypt, "Env-Provided-Pass-1").run();

        assertThat(admin.getPassword()).isEqualTo(customHash);
        assertThat(admin.isMustChangePassword()).isFalse();
        verify(repository, never()).save(any());
    }

    @Test
    void rejectsWeakOrDefaultEnvPassword() {
        when(repository.findByUsername("admin")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new DatabaseInitializer(repository, bcrypt, "admin123").run())
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new DatabaseInitializer(repository, bcrypt, "short").run())
                .isInstanceOf(IllegalStateException.class);
    }

    private UserAccount captureSaved() {
        ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }

    private static UserAccount admin(String hash) {
        return UserAccount.builder().username("admin").password(hash).role("ADMIN").build();
    }
}
