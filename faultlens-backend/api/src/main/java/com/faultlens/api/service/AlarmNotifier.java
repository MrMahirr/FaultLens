package com.faultlens.api.service;

import com.faultlens.api.model.AlarmChannel;
import com.faultlens.api.model.NotificationSettings;
import com.faultlens.api.model.UserAccount;
import com.faultlens.api.notification.Notification;
import com.faultlens.api.notification.NotificationChannel;
import com.faultlens.api.notification.Recipient;
import com.faultlens.api.repository.NotificationSettingsRepository;
import com.faultlens.api.repository.UserAccountRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Announces a raised alarm on the requested {@link NotificationChannel channels} (P07).
 *
 * <p>{@code @Async}: these are outgoing network calls (Slack, SMTP, webhooks) that must not hold
 * up alarm processing. Broadcast channels are sent once; personal channels go to every user who
 * opted in. Each delivery is retried a few times; one channel or recipient failing never stops
 * the others.
 */
@Slf4j
@Service
public class AlarmNotifier {
    private final NotificationSettingsRepository settingsRepository;
    private final UserAccountRepository userRepository;
    private final List<NotificationChannel> channels;
    private final int attempts;
    private final long backoffMs;

    public AlarmNotifier(
            NotificationSettingsRepository settingsRepository,
            UserAccountRepository userRepository,
            List<NotificationChannel> channels,
            @Value("${faultlens.alarms.notify-attempts:3}") int attempts,
            @Value("${faultlens.alarms.notify-backoff-ms:2000}") long backoffMs) {
        this.settingsRepository = settingsRepository;
        this.userRepository = userRepository;
        this.channels = List.copyOf(channels);
        this.attempts = Math.max(1, attempts);
        this.backoffMs = Math.max(0, backoffMs);
    }

    /** Notifies every channel (analysis alarms); runs on the async executor. */
    @Async
    public void notifyAll(String title, String message) {
        send(Notification.of(title, message), EnumSet.allOf(AlarmChannel.class));
    }

    /** Notifies only the given channels (an alarm rule's choice); runs on the async executor. */
    @Async
    public void notifyAll(String title, String message, Set<AlarmChannel> requested) {
        send(Notification.of(title, message), requested);
    }

    /** As above, with the alarm's severity shown in e-mails and webhook payloads. */
    @Async
    public void notifyAll(String title, String message, String severity, Set<AlarmChannel> requested) {
        send(new Notification(title, message, severity, Instant.now()), requested);
    }

    void send(Notification notification, Set<AlarmChannel> requested) {
        List<NotificationChannel> selected = channels.stream()
                .filter(channel -> requested.contains(channel.type()))
                .filter(this::available)
                .toList();

        List<NotificationChannel> personal = new ArrayList<>();
        for (NotificationChannel channel : selected) {
            if (channel.isBroadcast()) {
                deliver(channel, notification, null);
            } else {
                personal.add(channel);
            }
        }
        if (personal.isEmpty()) {
            return;
        }
        for (Recipient recipient : recipients()) {
            for (NotificationChannel channel : personal) {
                boolean accepted;
                try {
                    accepted = channel.accepts(recipient);
                } catch (Exception e) {
                    log.error("{} could not evaluate recipient {}", channel.type(), recipient.username(), e);
                    continue;
                }
                if (accepted) {
                    deliver(channel, notification, recipient);
                }
            }
        }
    }

    private boolean available(NotificationChannel channel) {
        try {
            return channel.isAvailable();
        } catch (Exception e) {
            log.error("{} availability check failed", channel.type(), e);
            return false;
        }
    }

    private List<Recipient> recipients() {
        try {
            List<NotificationSettings> allSettings = settingsRepository.findAll();
            Map<Long, UserAccount> users = userRepository.findAllById(
                            allSettings.stream().map(NotificationSettings::getUserId).toList())
                    .stream()
                    .collect(Collectors.toMap(UserAccount::getId, Function.identity()));
            List<Recipient> recipients = new ArrayList<>();
            for (NotificationSettings settings : allSettings) {
                UserAccount user = users.get(settings.getUserId());
                if (user != null) {
                    recipients.add(new Recipient(user.getUsername(), user.getEmail(), settings));
                }
            }
            return recipients;
        } catch (Exception e) {
            log.error("Could not load notification recipients", e);
            return List.of();
        }
    }

    private void deliver(NotificationChannel channel, Notification notification, Recipient recipient) {
        String label = channel.type() + (recipient == null ? "" : " → " + recipient.username());
        // Broadcasts (in-app) are local and not worth retrying.
        int tries = channel.isBroadcast() ? 1 : attempts;
        withRetry(label, tries, () -> channel.send(notification, recipient));
    }

    /** Tries up to {@code tries} times (send returned false or threw), backing off linearly. */
    private void withRetry(String label, int tries, BooleanSupplier send) {
        for (int attempt = 1; attempt <= tries; attempt++) {
            try {
                if (send.getAsBoolean()) {
                    return;
                }
                log.warn("{} alarm notification failed (attempt {}/{})", label, attempt, tries);
            } catch (Exception e) {
                log.warn("{} alarm notification failed (attempt {}/{})", label, attempt, tries, e);
            }
            if (attempt < tries && backoffMs > 0) {
                try {
                    Thread.sleep(backoffMs * attempt);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
        log.error("{} alarm notification gave up after {} attempts", label, tries);
    }
}
