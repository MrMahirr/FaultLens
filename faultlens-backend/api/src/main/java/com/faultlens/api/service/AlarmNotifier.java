package com.faultlens.api.service;

import com.faultlens.api.model.AlarmChannel;
import com.faultlens.api.model.NotificationSettings;
import com.faultlens.api.model.UserAccount;
import com.faultlens.api.repository.NotificationSettingsRepository;
import com.faultlens.api.repository.UserAccountRepository;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Sends a raised alarm to every channel (in-app, Slack, e-mail).
 *
 * <p>{@link #notifyAll} is {@code @Async}: these are outgoing HTTP calls (Slack, EmailJS) that
 * used to run on the Kafka listener thread, so a slow or failing provider held up alarm
 * processing. Slack and e-mail are retried a few times; one channel failing never stops the others.
 */
@Slf4j
@Service
public class AlarmNotifier {
    private final NotificationSettingsRepository settingsRepository;
    private final UserAccountRepository userRepository;
    private final SlackNotificationService slackNotificationService;
    private final EmailNotificationService emailNotificationService;
    private final InAppNotificationService inAppNotificationService;
    private final int attempts;
    private final long backoffMs;

    public AlarmNotifier(
            NotificationSettingsRepository settingsRepository,
            UserAccountRepository userRepository,
            SlackNotificationService slackNotificationService,
            EmailNotificationService emailNotificationService,
            InAppNotificationService inAppNotificationService,
            @Value("${faultlens.alarms.notify-attempts:3}") int attempts,
            @Value("${faultlens.alarms.notify-backoff-ms:2000}") long backoffMs) {
        this.settingsRepository = settingsRepository;
        this.userRepository = userRepository;
        this.slackNotificationService = slackNotificationService;
        this.emailNotificationService = emailNotificationService;
        this.inAppNotificationService = inAppNotificationService;
        this.attempts = Math.max(1, attempts);
        this.backoffMs = Math.max(0, backoffMs);
    }

    /**
     * Notifies all channels; runs on the async executor.
     */
    @Async
    public void notifyAll(String title, String message) {
        send(title, message, EnumSet.allOf(AlarmChannel.class));
    }

    /**
     * Notifies only the given channels (an alarm rule's choice); runs on the async executor.
     */
    @Async
    public void notifyAll(String title, String message, Set<AlarmChannel> channels) {
        send(title, message, channels);
    }

    private void send(String title, String message, Set<AlarmChannel> channels) {
        if (channels.contains(AlarmChannel.IN_APP)) {
            try {
                inAppNotificationService.broadcastAlarm(title, message);
            } catch (Exception e) {
                log.error("In-App alarm broadcast failed", e);
            }
        }
        if (!channels.contains(AlarmChannel.SLACK) && !channels.contains(AlarmChannel.EMAIL)) {
            return;
        }

        List<NotificationSettings> allSettings = settingsRepository.findAll();
        for (NotificationSettings settings : allSettings) {
            if (channels.contains(AlarmChannel.SLACK) && settings.isSlackEnabled() && settings.getSlackWebhook() != null) {
                withRetry("Slack", () -> slackNotificationService.sendNotification(
                        settings.getSlackWebhook(), title + "\n" + message));
            }
            if (channels.contains(AlarmChannel.EMAIL) && settings.isEmailEnabled() && settings.getEmailjsServiceId() != null) {
                Optional<UserAccount> user = findUser(settings.getUserId());
                if (user.isPresent() && user.get().getEmail() != null) {
                    String to = user.get().getEmail();
                    withRetry("Email", () -> emailNotificationService.sendEmail(to, title, message,
                            settings.getEmailjsServiceId(), settings.getEmailjsTemplateId(),
                            settings.getEmailjsPublicKey()));
                }
            }
        }
    }

    private Optional<UserAccount> findUser(Long userId) {
        try {
            return userRepository.findById(userId);
        } catch (Exception e) {
            log.error("Could not load user {} for e-mail notification", userId, e);
            return Optional.empty();
        }
    }

    /** Tries up to {@code attempts} times (send returned false or threw), backing off linearly. */
    private void withRetry(String channel, BooleanSupplier send) {
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                if (send.getAsBoolean()) {
                    return;
                }
                log.warn("{} alarm notification failed (attempt {}/{})", channel, attempt, attempts);
            } catch (Exception e) {
                log.warn("{} alarm notification failed (attempt {}/{})", channel, attempt, attempts, e);
            }
            if (attempt < attempts && backoffMs > 0) {
                try {
                    Thread.sleep(backoffMs * attempt);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
        log.error("{} alarm notification gave up after {} attempts", channel, attempts);
    }
}
