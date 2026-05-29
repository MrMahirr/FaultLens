package com.faultlens.api.config;

import com.faultlens.api.model.UserAccount;
import com.faultlens.api.repository.UserAccountRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DatabaseInitializer implements CommandLineRunner {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Create default admin user if no users exist or specifically admin doesn't exist
        if (userAccountRepository.findByUsername("admin").isEmpty()) {
            log.info("Default admin user not found. Creating 'admin' account...");
            UserAccount admin = UserAccount.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .role("ADMIN")
                    .createdAt(Instant.now())
                    .build();
            userAccountRepository.save(admin);
            log.info("Default admin user created successfully (username: admin, password: admin123).");
        } else {
            log.info("Default admin user already exists.");
        }
    }
}
