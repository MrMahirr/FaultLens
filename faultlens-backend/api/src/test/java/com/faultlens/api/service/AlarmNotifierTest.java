package com.faultlens.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.faultlens.api.model.AlarmChannel;
import com.faultlens.api.model.NotificationSettings;
import com.faultlens.api.model.UserAccount;
import com.faultlens.api.notification.EmailChannel;
import com.faultlens.api.notification.InAppChannel;
import com.faultlens.api.notification.Notification;
import com.faultlens.api.notification.Recipient;
import com.faultlens.api.notification.SlackChannel;
import com.faultlens.api.notification.WebhookChannel;
import com.faultlens.api.repository.NotificationSettingsRepository;
import com.faultlens.api.repository.UserAccountRepository;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AlarmNotifierTest {
    private static final String TITLE = "Yeni Kritik Hata Tespit Edildi!";
    private static final String MESSAGE = "Hata Analizi (Grup ID: 42)";

    private NotificationSettingsRepository settingsRepository;
    private UserAccountRepository userRepository;
    private SlackNotificationService slack;
    private InAppNotificationService inApp;
    private EmailChannel email;
    private WebhookChannel webhook;
    private AlarmNotifier notifier;

    @BeforeEach
    void setUp() {
        settingsRepository = mock(NotificationSettingsRepository.class);
        userRepository = mock(UserAccountRepository.class);
        slack = mock(SlackNotificationService.class);
        inApp = mock(InAppNotificationService.class);
        email = mock(EmailChannel.class);
        webhook = mock(WebhookChannel.class);
        when(email.type()).thenReturn(AlarmChannel.EMAIL);
        when(email.isAvailable()).thenReturn(true);
        when(email.accepts(any())).thenAnswer(inv -> {
            Recipient r = inv.getArgument(0);
            return r.settings().isEmailEnabled() && r.email() != null;
        });
        when(email.send(any(), any())).thenReturn(true);
        when(webhook.type()).thenReturn(AlarmChannel.WEBHOOK);
        when(webhook.isAvailable()).thenReturn(true);
        when(settingsRepository.findAll()).thenReturn(List.of());
        when(slack.sendNotification(anyString(), anyString())).thenReturn(true);
        // Real Slack/in-app channels over mocked transports; 3 attempts, no sleeping in tests.
        notifier = new AlarmNotifier(settingsRepository, userRepository,
                List.of(new InAppChannel(inApp), new SlackChannel(slack), email, webhook), 3, 0);
    }

    private void users(UserAccount... accounts) {
        when(userRepository.findAllById(any())).thenReturn(List.of(accounts));
    }

    @Test
    void alwaysBroadcastsInApp() {
        notifier.notifyAll(TITLE, MESSAGE);

        verify(inApp).broadcastAlarm(eq(TITLE), contains("Grup ID: 42"));
    }

    @Test
    void notifiesOnlyTheChannelsEachUserEnabled() {
        when(settingsRepository.findAll()).thenReturn(List.of(
                settings(1L, true, "https://hooks.slack.com/a", false),
                settings(2L, false, "https://hooks.slack.com/b", true),
                settings(3L, true, null, true)));
        users(user(1L, null), user(2L, "ops@example.com"), user(3L, null));

        notifier.notifyAll(TITLE, MESSAGE);

        verify(slack).sendNotification(eq("https://hooks.slack.com/a"), contains("Grup ID: 42"));
        verify(slack, never()).sendNotification(eq("https://hooks.slack.com/b"), anyString());
        ArgumentCaptor<Recipient> to = ArgumentCaptor.forClass(Recipient.class);
        verify(email).send(any(Notification.class), to.capture());
        // user 3: e-mail on but no address; slack on but no webhook → nothing
        assertThat(to.getAllValues()).extracting(Recipient::email).containsExactly("ops@example.com");
    }

    @Test
    void ruleAlarmsUseOnlyTheRulesChannelsAndCarryTheSeverity() {
        when(settingsRepository.findAll()).thenReturn(List.of(settings(2L, true, "https://hooks.slack.com/b", true)));
        users(user(2L, "ops@example.com"));

        notifier.notifyAll("Alarm: x", MESSAGE, "ERROR", EnumSet.of(AlarmChannel.EMAIL));

        verify(inApp, never()).broadcastAlarm(anyString(), anyString());
        verify(slack, never()).sendNotification(anyString(), anyString());
        ArgumentCaptor<Notification> sent = ArgumentCaptor.forClass(Notification.class);
        verify(email).send(sent.capture(), any());
        assertThat(sent.getValue().severity()).isEqualTo("ERROR");
    }

    @Test
    void skipsChannelsTheServerIsNotSetUpFor() {
        when(email.isAvailable()).thenReturn(false);
        when(settingsRepository.findAll()).thenReturn(List.of(settings(2L, false, null, true)));
        users(user(2L, "ops@example.com"));

        notifier.notifyAll(TITLE, MESSAGE);

        verify(email, never()).send(any(), any());
    }

    @Test
    void aFailingChannelDoesNotStopTheOthers() {
        doThrow(new RuntimeException("ws down")).when(inApp).broadcastAlarm(anyString(), anyString());
        when(slack.sendNotification(eq("https://hooks.slack.com/a"), anyString())).thenThrow(new RuntimeException("slack down"));
        when(settingsRepository.findAll()).thenReturn(List.of(
                settings(1L, true, "https://hooks.slack.com/a", true),
                settings(2L, true, "https://hooks.slack.com/b", false)));
        users(user(1L, "a@example.com"), user(2L, null));

        notifier.notifyAll(TITLE, MESSAGE);

        verify(email).send(any(), any());
        verify(slack).sendNotification(eq("https://hooks.slack.com/b"), anyString());
    }

    @Test
    void retriesAFailedSendAndStopsOnSuccess() {
        when(settingsRepository.findAll()).thenReturn(List.of(settings(1L, true, "https://hooks.slack.com/a", false)));
        users(user(1L, null));
        when(slack.sendNotification(anyString(), anyString())).thenReturn(false, true);

        notifier.notifyAll(TITLE, MESSAGE);

        verify(slack, times(2)).sendNotification(eq("https://hooks.slack.com/a"), anyString());
    }

    @Test
    void givesUpAfterTheConfiguredAttempts() {
        when(settingsRepository.findAll()).thenReturn(List.of(settings(1L, true, "https://hooks.slack.com/a", false)));
        users(user(1L, null));
        when(slack.sendNotification(anyString(), anyString())).thenThrow(new RuntimeException("down"));

        notifier.notifyAll(TITLE, MESSAGE);

        verify(slack, times(3)).sendNotification(eq("https://hooks.slack.com/a"), anyString());
    }

    private static NotificationSettings settings(Long userId, boolean slackOn, String webhook, boolean emailOn) {
        NotificationSettings settings = new NotificationSettings();
        settings.setUserId(userId);
        settings.setSlackEnabled(slackOn);
        settings.setSlackWebhook(webhook);
        settings.setEmailEnabled(emailOn);
        return settings;
    }

    private static UserAccount user(Long id, String address) {
        UserAccount user = new UserAccount();
        user.setId(id);
        user.setUsername("user" + id);
        user.setEmail(address);
        return user;
    }
}
