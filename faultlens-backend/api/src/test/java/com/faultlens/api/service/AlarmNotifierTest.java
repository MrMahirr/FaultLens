package com.faultlens.api.service;

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

import com.faultlens.api.model.NotificationSettings;
import com.faultlens.api.model.UserAccount;
import com.faultlens.api.repository.NotificationSettingsRepository;
import com.faultlens.api.repository.UserAccountRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AlarmNotifierTest {
    private static final String TITLE = "Yeni Kritik Hata Tespit Edildi!";
    private static final String MESSAGE = "Hata Analizi (Grup ID: 42)";

    private NotificationSettingsRepository settingsRepository;
    private UserAccountRepository userRepository;
    private SlackNotificationService slack;
    private EmailNotificationService email;
    private InAppNotificationService inApp;
    private AlarmNotifier notifier;

    @BeforeEach
    void setUp() {
        settingsRepository = mock(NotificationSettingsRepository.class);
        userRepository = mock(UserAccountRepository.class);
        slack = mock(SlackNotificationService.class);
        email = mock(EmailNotificationService.class);
        inApp = mock(InAppNotificationService.class);
        when(settingsRepository.findAll()).thenReturn(List.of());
        when(slack.sendNotification(anyString(), anyString())).thenReturn(true);
        when(email.sendEmail(any(), any(), any(), any(), any(), any())).thenReturn(true);
        // 3 attempts, no sleeping in tests
        notifier = new AlarmNotifier(settingsRepository, userRepository, slack, email, inApp, 3, 0);
    }

    @Test
    void alwaysBroadcastsInApp() {
        notifier.notifyAll(TITLE, MESSAGE);

        verify(inApp).broadcastAlarm(eq(TITLE), contains("Grup ID: 42"));
    }

    @Test
    void notifiesOnlyTheChannelsEachUserEnabled() {
        when(settingsRepository.findAll()).thenReturn(List.of(
                settings(1L, true, "https://hooks.slack.com/a", false, null),
                settings(2L, false, "https://hooks.slack.com/b", true, "svc"),
                settings(3L, true, null, true, null)));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, "ops@example.com")));

        notifier.notifyAll(TITLE, MESSAGE);

        verify(slack).sendNotification(eq("https://hooks.slack.com/a"), contains("Grup ID: 42"));
        verify(slack, never()).sendNotification(eq("https://hooks.slack.com/b"), anyString());
        verify(email).sendEmail(eq("ops@example.com"), eq(TITLE), contains("Grup ID: 42"),
                eq("svc"), eq("tpl"), eq("pub"));
        // user 3: slack on but no webhook, email on but no EmailJS service → nothing
        verify(userRepository, never()).findById(3L);
    }

    @Test
    void skipsEmailWhenTheUserHasNoAddress() {
        when(settingsRepository.findAll()).thenReturn(List.of(settings(2L, false, null, true, "svc")));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, null)));

        notifier.notifyAll(TITLE, MESSAGE);

        verify(email, never()).sendEmail(any(), any(), any(), any(), any(), any());
    }

    @Test
    void aFailingChannelDoesNotStopTheOthers() {
        doThrow(new RuntimeException("ws down")).when(inApp).broadcastAlarm(anyString(), anyString());
        when(slack.sendNotification(eq("https://hooks.slack.com/a"), anyString())).thenThrow(new RuntimeException("slack down"));
        when(settingsRepository.findAll()).thenReturn(List.of(
                settings(1L, true, "https://hooks.slack.com/a", true, "svc"),
                settings(2L, true, "https://hooks.slack.com/b", false, null)));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, "a@example.com")));

        notifier.notifyAll(TITLE, MESSAGE);

        verify(email).sendEmail(eq("a@example.com"), anyString(), anyString(), eq("svc"), eq("tpl"), eq("pub"));
        verify(slack).sendNotification(eq("https://hooks.slack.com/b"), anyString());
    }

    @Test
    void retriesAFailedSendAndStopsOnSuccess() {
        when(settingsRepository.findAll()).thenReturn(List.of(settings(1L, true, "https://hooks.slack.com/a", false, null)));
        when(slack.sendNotification(anyString(), anyString())).thenReturn(false, true);

        notifier.notifyAll(TITLE, MESSAGE);

        verify(slack, times(2)).sendNotification(eq("https://hooks.slack.com/a"), anyString());
    }

    @Test
    void givesUpAfterTheConfiguredAttempts() {
        when(settingsRepository.findAll()).thenReturn(List.of(settings(1L, true, "https://hooks.slack.com/a", false, null)));
        when(slack.sendNotification(anyString(), anyString())).thenThrow(new RuntimeException("down"));

        notifier.notifyAll(TITLE, MESSAGE);

        verify(slack, times(3)).sendNotification(eq("https://hooks.slack.com/a"), anyString());
    }

    private static NotificationSettings settings(Long userId, boolean slackOn, String webhook, boolean emailOn, String serviceId) {
        NotificationSettings settings = new NotificationSettings();
        settings.setUserId(userId);
        settings.setSlackEnabled(slackOn);
        settings.setSlackWebhook(webhook);
        settings.setEmailEnabled(emailOn);
        settings.setEmailjsServiceId(serviceId);
        settings.setEmailjsTemplateId("tpl");
        settings.setEmailjsPublicKey("pub");
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
