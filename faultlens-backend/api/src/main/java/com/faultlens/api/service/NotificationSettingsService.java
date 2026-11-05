package com.faultlens.api.service;

import com.faultlens.api.dto.SettingsDtos.NotificationSettingsResponse;
import com.faultlens.api.dto.SettingsDtos.UpdateNotificationRequest;
import com.faultlens.api.model.NotificationSettings;
import com.faultlens.api.model.UserAccount;
import com.faultlens.api.notification.EmailChannel;
import com.faultlens.api.notification.WebhookChannel;
import com.faultlens.api.repository.NotificationSettingsRepository;
import com.faultlens.api.repository.UserAccountRepository;
import com.faultlens.common.exception.FaultLensException;
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
    private final EmailChannel emailChannel;
    private final WebhookChannel webhookChannel;

    /**
     * Returns notification preferences for the given user.
     * Creates default settings if none exist.
     */
    @Transactional
    public NotificationSettingsResponse getByUsername(String username) {
        UserAccount user = findUser(username);
        NotificationSettings settings = settingsRepository.findByUserId(user.getId())
                .orElseGet(() -> createDefault(user.getId()));
        return toResponse(settings, user);
    }

    /**
     * Updates notification preferences for the given user.
     */
    @Transactional
    public NotificationSettingsResponse update(String username, UpdateNotificationRequest request) {
        UserAccount user = findUser(username);
        NotificationSettings settings = settingsRepository.findByUserId(user.getId())
                .orElseGet(() -> createDefault(user.getId()));

        String slackWebhook = request.slackWebhook();
        if (slackWebhook != null && !slackWebhook.isBlank() && !SlackWebhookUrls.isAllowed(slackWebhook)) {
            throw new FaultLensException(
                    "Slack webhook must be an https://hooks.slack.com/... URL", "INVALID_SLACK_WEBHOOK");
        }
        String webhookUrl = blankToNull(request.webhookUrl());
        if (webhookUrl != null && !webhookChannel.isAllowed(webhookUrl)) {
            throw new FaultLensException(
                    "Webhook must be an https URL on a public host", "INVALID_WEBHOOK_URL");
        }

        settings.setEmailEnabled(request.emailEnabled());
        settings.setSlackEnabled(request.slackEnabled());
        settings.setSlackWebhook(request.slackWebhook());
        settings.setPushEnabled(request.pushEnabled());
        settings.setMobileEnabled(request.mobileEnabled());
        settings.setWebhookEnabled(request.webhookEnabled());
        settings.setWebhookUrl(webhookUrl);
        settings.setUpdatedAt(Instant.now());

        NotificationSettings saved = settingsRepository.save(settings);
        return toResponse(saved, user);
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

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private NotificationSettingsResponse toResponse(NotificationSettings settings, UserAccount user) {
        return new NotificationSettingsResponse(
                settings.isEmailEnabled(),
                settings.isSlackEnabled(),
                settings.getSlackWebhook(),
                settings.isWebhookEnabled(),
                settings.getWebhookUrl(),
                settings.isPushEnabled(),
                settings.isMobileEnabled(),
                user.getEmail(),
                emailChannel.isAvailable()
        );
    }
}
