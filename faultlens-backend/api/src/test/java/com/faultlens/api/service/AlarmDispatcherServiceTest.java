package com.faultlens.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.faultlens.api.model.Alarm;
import com.faultlens.api.model.AlarmStatus;
import com.faultlens.api.model.NotificationSettings;
import com.faultlens.api.model.UserAccount;
import com.faultlens.api.repository.AlarmRepository;
import com.faultlens.api.repository.NotificationSettingsRepository;
import com.faultlens.api.repository.UserAccountRepository;
import com.faultlens.common.dto.AnalysisResultDto;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AlarmDispatcherServiceTest {
    private AlarmRepository alarmRepository;
    private NotificationSettingsRepository settingsRepository;
    private UserAccountRepository userRepository;
    private SlackNotificationService slack;
    private EmailNotificationService email;
    private InAppNotificationService inApp;
    private AlarmDispatcherService dispatcher;

    @BeforeEach
    void setUp() {
        alarmRepository = mock(AlarmRepository.class);
        settingsRepository = mock(NotificationSettingsRepository.class);
        userRepository = mock(UserAccountRepository.class);
        slack = mock(SlackNotificationService.class);
        email = mock(EmailNotificationService.class);
        inApp = mock(InAppNotificationService.class);
        when(alarmRepository.save(any(Alarm.class))).thenAnswer(inv -> inv.getArgument(0));
        when(settingsRepository.findAll()).thenReturn(List.of());
        dispatcher = new AlarmDispatcherService(alarmRepository, settingsRepository, userRepository, slack, email, inApp);
    }

    @Test
    void savesAnActiveAlarmCarryingTheAnalysisSource() {
        dispatcher.dispatchAlarm(analysis(0.9));

        Alarm alarm = savedAlarm();
        assertThat(alarm.getSourceId()).as("regression: used to receive the log group id").isEqualTo(5L);
        assertThat(alarm.getRuleId()).isEqualTo("AI_ANALYSIS");
        assertThat(alarm.getStatus()).isEqualTo(AlarmStatus.ACTIVE);
        assertThat(alarm.getTriggeredAt()).isNotNull();
        assertThat(alarm.getMessage())
                .contains("Grup ID: 42")
                .contains("Kök Neden: DB down")
                .contains("Öneri: restart pool")
                .contains("%90");
    }

    @Test
    void severityIsCriticalOnlyAboveEightyPercentConfidence() {
        dispatcher.dispatchAlarm(analysis(0.81));
        dispatcher.dispatchAlarm(analysis(0.8));
        dispatcher.dispatchAlarm(analysis(null));

        ArgumentCaptor<Alarm> captor = ArgumentCaptor.forClass(Alarm.class);
        verify(alarmRepository, org.mockito.Mockito.times(3)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(Alarm::getSeverity).containsExactly("CRITICAL", "ERROR", "ERROR");
        assertThat(captor.getAllValues().get(2).getMessage()).contains("%0");
    }

    @Test
    void alwaysBroadcastsInApp() {
        dispatcher.dispatchAlarm(analysis(0.5));

        verify(inApp).broadcastAlarm(eq("Yeni Kritik Hata Tespit Edildi!"), contains("Grup ID: 42"));
    }

    @Test
    void notifiesOnlyTheChannelsEachUserEnabled() {
        when(settingsRepository.findAll()).thenReturn(List.of(
                settings(1L, true, "https://hooks.slack.com/a", false, null),
                settings(2L, false, "https://hooks.slack.com/b", true, "svc"),
                settings(3L, true, null, true, null)));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, "ops@example.com")));

        dispatcher.dispatchAlarm(analysis(0.9));

        verify(slack).sendNotification(eq("https://hooks.slack.com/a"), contains("Grup ID: 42"));
        verify(slack, never()).sendNotification(eq("https://hooks.slack.com/b"), anyString());
        verify(email).sendEmail(eq("ops@example.com"), eq("Yeni Kritik Hata Tespit Edildi!"), contains("Grup ID: 42"),
                eq("svc"), eq("tpl"), eq("pub"));
        // user 3: slack on but no webhook, email on but no EmailJS service → nothing
        verify(userRepository, never()).findById(3L);
    }

    @Test
    void skipsEmailWhenTheUserHasNoAddress() {
        when(settingsRepository.findAll()).thenReturn(List.of(settings(2L, false, null, true, "svc")));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, null)));

        dispatcher.dispatchAlarm(analysis(0.9));

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

        dispatcher.dispatchAlarm(analysis(0.9));

        verify(alarmRepository).save(any(Alarm.class));
        verify(email).sendEmail(eq("a@example.com"), anyString(), anyString(), eq("svc"), eq("tpl"), eq("pub"));
        verify(slack).sendNotification(eq("https://hooks.slack.com/b"), anyString());
    }

    private Alarm savedAlarm() {
        ArgumentCaptor<Alarm> captor = ArgumentCaptor.forClass(Alarm.class);
        verify(alarmRepository).save(captor.capture());
        return captor.getValue();
    }

    private static AnalysisResultDto analysis(Double confidence) {
        return AnalysisResultDto.builder()
                .id(99L)
                .logGroupId(42L)
                .sourceId(5L)
                .rootCause("DB down")
                .suggestion("restart pool")
                .confidenceScore(confidence)
                .engineType("RULE_BASED")
                .build();
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
