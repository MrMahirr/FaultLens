package com.faultlens.api.config;

import com.faultlens.api.model.UserAccount;
import com.faultlens.api.repository.UserAccountRepository;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Ensures an {@code admin} account exists and never runs with a well-known password.
 *
 * <ul>
 *   <li>No admin yet: create it with {@code ADMIN_INITIAL_PASSWORD}, or a generated password
 *       that is logged once.</li>
 *   <li>Admin still on a public default ({@code V6__create_users.sql} seed hash or
 *       {@code admin123}): reset it the same way.</li>
 * </ul>
 * In both cases the admin must choose a new password at first login.
 */
@Configuration
@Slf4j
public class DatabaseInitializer implements CommandLineRunner {
    static final String ADMIN_USERNAME = "admin";
    /** Hash seeded by V6__create_users.sql; it is a widely published example hash. */
    static final String V6_SEED_HASH = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
    /** Passwords that were documented as defaults and must never stay active. */
    static final Set<String> KNOWN_DEFAULT_PASSWORDS = Set.of("admin123", "admin", "password");

    private static final String PASSWORD_ALPHABET =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private static final int GENERATED_PASSWORD_LENGTH = 20;

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final String initialPassword;
    private final SecureRandom random = new SecureRandom();

    public DatabaseInitializer(
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder,
            @Value("${faultlens.security.admin-initial-password:}") String initialPassword) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.initialPassword = initialPassword;
    }

    @Override
    @Transactional
    public void run(String... args) {
        userAccountRepository.findByUsername(ADMIN_USERNAME).ifPresentOrElse(
                admin -> {
                    if (usesKnownDefaultPassword(admin)) {
                        log.warn("Admin account uses a well-known default password; resetting it.");
                        admin.setPassword(passwordEncoder.encode(resolveInitialPassword()));
                        admin.setMustChangePassword(true);
                        userAccountRepository.save(admin);
                    }
                },
                () -> {
                    log.info("No '{}' account found; creating it.", ADMIN_USERNAME);
                    userAccountRepository.save(UserAccount.builder()
                            .username(ADMIN_USERNAME)
                            .password(passwordEncoder.encode(resolveInitialPassword()))
                            .role("ADMIN")
                            .mustChangePassword(true)
                            .createdAt(Instant.now())
                            .build());
                });
    }

    boolean usesKnownDefaultPassword(UserAccount admin) {
        String hash = admin.getPassword();
        if (hash == null || V6_SEED_HASH.equals(hash)) {
            return true;
        }
        return KNOWN_DEFAULT_PASSWORDS.stream().anyMatch(candidate -> passwordEncoder.matches(candidate, hash));
    }

    private String resolveInitialPassword() {
        if (StringUtils.hasText(initialPassword)) {
            if (KNOWN_DEFAULT_PASSWORDS.contains(initialPassword) || initialPassword.length() < 8) {
                throw new IllegalStateException(
                        "ADMIN_INITIAL_PASSWORD must be at least 8 characters and not a well-known default");
            }
            log.info("Admin password set from ADMIN_INITIAL_PASSWORD; it must be changed at first login.");
            return initialPassword;
        }
        String generated = generatePassword();
        // Logged once, like other self-hosted tools' initial admin password; it is single-use
        // because the admin must change it at first login.
        log.warn("""

                ================================================================
                  Generated initial admin password (change it at first login):
                    username: {}
                    password: {}
                  Set ADMIN_INITIAL_PASSWORD to choose it yourself.
                ================================================================""",
                ADMIN_USERNAME, generated);
        return generated;
    }

    private String generatePassword() {
        StringBuilder sb = new StringBuilder(GENERATED_PASSWORD_LENGTH);
        for (int i = 0; i < GENERATED_PASSWORD_LENGTH; i++) {
            sb.append(PASSWORD_ALPHABET.charAt(random.nextInt(PASSWORD_ALPHABET.length())));
        }
        return sb.toString();
    }
}
