package com.faultlens.api.repository;

import com.faultlens.api.model.NotificationSettings;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationSettingsRepository extends JpaRepository<NotificationSettings, Long> {
    /**
     * Finds notification settings by user ID.
     */
    Optional<NotificationSettings> findByUserId(Long userId);
}
