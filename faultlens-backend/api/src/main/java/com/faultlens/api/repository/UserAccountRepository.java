package com.faultlens.api.repository;

import com.faultlens.api.model.UserAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    /**
     * Finds a user account by username.
     */
    Optional<UserAccount> findByUsername(String username);
}
