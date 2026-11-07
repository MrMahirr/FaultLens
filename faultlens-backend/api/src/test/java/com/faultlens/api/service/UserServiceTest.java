package com.faultlens.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.faultlens.api.dto.UserDtos.CreateUserRequest;
import com.faultlens.api.model.UserAccount;
import com.faultlens.api.repository.UserAccountRepository;
import com.faultlens.api.security.RefreshTokenStore;
import com.faultlens.common.exception.ConflictException;
import com.faultlens.common.exception.FaultLensException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class UserServiceTest {
    private UserAccountRepository repository;
    private UserService service;
    private RefreshTokenStore refreshTokens;

    @BeforeEach
    void setUp() {
        repository = mock(UserAccountRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(encoder.encode(any())).thenAnswer(inv -> "hashed:" + inv.getArgument(0));
        when(repository.save(any(UserAccount.class))).thenAnswer(inv -> inv.getArgument(0));
        when(repository.saveAndFlush(any(UserAccount.class))).thenAnswer(inv -> {
            UserAccount user = inv.getArgument(0);
            user.setId(9L);
            return user;
        });
        refreshTokens = mock(RefreshTokenStore.class);
        service = new UserService(repository, encoder, refreshTokens);
    }

    private static UserAccount user(long id, String username, String role) {
        return UserAccount.builder().id(id).username(username).role(role).password("x").build();
    }

    @Test
    void createsAccountsThatMustChangeTheirPassword() {
        var created = service.create(new CreateUserRequest(" ayse ", " ", "VIEWER", "initial-pass"));

        assertThat(created.username()).isEqualTo("ayse");
        assertThat(created.email()).isNull();
        assertThat(created.role()).isEqualTo("VIEWER");
        assertThat(created.mustChangePassword()).isTrue();
        assertThat(created.createdAt()).isNotNull();
    }

    @Test
    void rejectsTakenUsernames() {
        when(repository.findByUsername("admin")).thenReturn(Optional.of(user(1, "admin", "ADMIN")));

        assertThatThrownBy(() -> service.create(new CreateUserRequest("admin", null, "EDITOR", "initial-pass")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void neverLeavesTheSystemWithoutAnAdmin() {
        when(repository.findById(1L)).thenReturn(Optional.of(user(1, "root", "ADMIN")));
        when(repository.countByRole("ADMIN")).thenReturn(1L);

        assertThatThrownBy(() -> service.changeRole(1L, "EDITOR", "other"))
                .isInstanceOf(FaultLensException.class).extracting("errorCode").isEqualTo("LAST_ADMIN");
        assertThatThrownBy(() -> service.delete(1L, "other"))
                .isInstanceOf(FaultLensException.class).extracting("errorCode").isEqualTo("LAST_ADMIN");
        verify(repository, never()).delete(any());
    }

    @Test
    void adminsCannotDeleteOrReRoleThemselves() {
        when(repository.findById(1L)).thenReturn(Optional.of(user(1, "root", "ADMIN")));
        when(repository.countByRole("ADMIN")).thenReturn(3L);

        assertThatThrownBy(() -> service.delete(1L, "root"))
                .extracting("errorCode").isEqualTo("CANNOT_DELETE_SELF");
        assertThatThrownBy(() -> service.changeRole(1L, "VIEWER", "root"))
                .extracting("errorCode").isEqualTo("CANNOT_CHANGE_OWN_ROLE");
    }

    @Test
    void demotesAnAdminWhenAnotherRemainsAndRejectsUnknownRoles() {
        when(repository.findById(2L)).thenReturn(Optional.of(user(2, "mehmet", "ADMIN")));
        when(repository.countByRole("ADMIN")).thenReturn(2L);

        assertThat(service.changeRole(2L, "EDITOR", "root").role()).isEqualTo("EDITOR");
        assertThatThrownBy(() -> service.changeRole(2L, "OWNER", "root"))
                .extracting("errorCode").isEqualTo("INVALID_ROLE");
    }

    @Test
    void resetPasswordForcesAChangeAtNextLogin() {
        UserAccount stored = user(3, "ali", "EDITOR");
        when(repository.findById(3L)).thenReturn(Optional.of(stored));

        assertThat(service.resetPassword(3L, "temporary-1").mustChangePassword()).isTrue();
        assertThat(stored.getPassword()).isEqualTo("hashed:temporary-1");
        verify(refreshTokens).revokeAll(3L);
    }
}
