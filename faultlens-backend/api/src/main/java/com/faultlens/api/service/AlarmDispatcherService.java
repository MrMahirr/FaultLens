package com.faultlens.api.service;

import com.faultlens.common.dto.AnalysisResultDto;
import com.faultlens.api.model.Alarm;
import com.faultlens.api.model.NotificationSettings;
import com.faultlens.api.model.UserAccount;
import com.faultlens.api.repository.AlarmRepository;
import com.faultlens.api.repository.NotificationSettingsRepository;
import com.faultlens.api.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlarmDispatcherService {

    private final AlarmRepository alarmRepository;
    private final NotificationSettingsRepository settingsRepository;
    private final UserAccountRepository userRepository;
    private final SlackNotificationService slackNotificationService;
    private final EmailNotificationService emailNotificationService;
    private final InAppNotificationService inAppNotificationService;

    public void dispatchAlarm(AnalysisResultDto analysis) {
        log.info("Dispatching alarm for analysis ID: {}", analysis.getId());
        
        // Save alarm to database
        String severityLevel = analysis.getConfidenceScore() != null && analysis.getConfidenceScore() > 0.8 ? "CRITICAL" : "ERROR";
        
        Alarm alarm = Alarm.builder()
                .ruleId("AI_ANALYSIS")
                .ruleName("Yapay Zeka Kritik Hata Tespiti")
                .sourceId(analysis.getLogGroupId())
                .severity(severityLevel)
                .status("ACTIVE")
                .message(String.format("Hata Analizi (Grup ID: %d)\nKök Neden: %s\nÖneri: %s\nGüven Skoru: %%%.0f", 
                    analysis.getLogGroupId(), 
                    analysis.getRootCause(), 
                    analysis.getSuggestion(),
                    analysis.getConfidenceScore() != null ? analysis.getConfidenceScore() * 100 : 0.0))
                .triggeredAt(Instant.now())
                .build();
        
        alarmRepository.save(alarm);
        
        String title = "Yeni Kritik Hata Tespit Edildi!";
        String message = alarm.getMessage();

        // 1. In-App Broadcast (Her zaman çalışır)
        try {
            inAppNotificationService.broadcastAlarm(title, message);
        } catch (Exception e) {
            log.error("In-App alarm broadcast failed", e);
        }

        // 2. Fetch all users and their settings to notify them
        List<NotificationSettings> allSettings = settingsRepository.findAll();

        for (NotificationSettings settings : allSettings) {
            if (settings.isSlackEnabled() && settings.getSlackWebhook() != null) {
                try {
                    slackNotificationService.sendNotification(settings.getSlackWebhook(), title + "\n" + message);
                } catch (Exception e) {
                    log.error("Slack alarm send failed", e);
                }
            }

            if (settings.isEmailEnabled() && settings.getEmailjsServiceId() != null) {
                try {
                    Optional<UserAccount> user = userRepository.findById(settings.getUserId());
                    if (user.isPresent() && user.get().getEmail() != null) {
                        emailNotificationService.sendEmail(
                            user.get().getEmail(), 
                            title, 
                            message, 
                            settings.getEmailjsServiceId(), 
                            settings.getEmailjsTemplateId(), 
                            settings.getEmailjsPublicKey()
                        );
                    }
                } catch (Exception e) {
                    log.error("Email alarm send failed", e);
                }
            }
        }
    }
}
