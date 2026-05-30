package com.faultlens.api.service;

import com.faultlens.api.dto.SettingsDtos.NotificationSettingsResponse;
import com.faultlens.api.dto.SettingsDtos.UpdateNotificationRequest;
import com.faultlens.api.model.NotificationSettings;
import com.faultlens.api.model.UserAccount;
import com.faultlens.api.repository.NotificationSettingsRepository;
import com.faultlens.api.repository.UserAccountRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationSettingsService {

    private final NotificationSettingsRepository settingsRepository;
    private final UserAccountRepository userRepository;

    /**
     * Returns notification preferences for the given user.
     * Creates default settings if none exist.
     */
    @Transactional
    public NotificationSettingsResponse getByUsername(String username) {
        UserAccount user = findUser(username);
        NotificationSettings settings = settingsRepository.findByUserId(user.getId())
                .orElseGet(() -> createDefault(user.getId()));
        return toResponse(settings);
    }

    /**
     * Updates notification preferences for the given user.
     */
    @Transactional
    public NotificationSettingsResponse update(String username, UpdateNotificationRequest request) {
        UserAccount user = findUser(username);
        NotificationSettings settings = settingsRepository.findByUserId(user.getId())
                .orElseGet(() -> createDefault(user.getId()));

        settings.setEmailEnabled(request.emailEnabled());
        settings.setSlackEnabled(request.slackEnabled());
        settings.setSlackWebhook(request.slackWebhook());
        settings.setPushEnabled(request.pushEnabled());
        settings.setMobileEnabled(request.mobileEnabled());
        settings.setUpdatedAt(Instant.now());

        NotificationSettings saved = settingsRepository.save(settings);
        return toResponse(saved);
    }

    private UserAccount findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("User not found"));
    }

    private NotificationSettings createDefault(Long userId) {
        NotificationSettings defaults = NotificationSettings.builder()
                .userId(userId)
                .emailEnabled(true)
                .slackEnabled(false)
                .pushEnabled(false)
                .mobileEnabled(false)
                .updatedAt(Instant.now())
                .build();
        return settingsRepository.save(defaults);
    }

    private NotificationSettingsResponse toResponse(NotificationSettings settings) {
        return new NotificationSettingsResponse(
                settings.isEmailEnabled(),
                settings.isSlackEnabled(),
                settings.getSlackWebhook(),
                settings.isPushEnabled(),
                settings.isMobileEnabled()
        );
    }
}
