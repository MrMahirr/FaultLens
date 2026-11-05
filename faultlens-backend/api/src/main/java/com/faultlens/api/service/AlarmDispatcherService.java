package com.faultlens.api.service;

import com.faultlens.api.model.Alarm;
import com.faultlens.api.model.AlarmStatus;
import com.faultlens.api.repository.AlarmRepository;
import com.faultlens.common.dto.AnalysisResultDto;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Turns an analysis into an alarm: stores it (synchronously, so it is never lost) and hands the
 * notifications to {@link AlarmNotifier}, which sends them asynchronously.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlarmDispatcherService {
    static final String TITLE = "Yeni Kritik Hata Tespit Edildi!";

    private final AlarmRepository alarmRepository;
    private final AlarmNotifier notifier;

    public void dispatchAlarm(AnalysisResultDto analysis) {
        log.info("Dispatching alarm for analysis ID: {}", analysis.getId());

        String severityLevel = analysis.getConfidenceScore() != null && analysis.getConfidenceScore() > 0.8
                ? "CRITICAL" : "ERROR";

        Alarm alarm = Alarm.builder()
                .ruleId("AI_ANALYSIS")
                .ruleName("Yapay Zeka Kritik Hata Tespiti")
                .sourceId(analysis.getSourceId())
                .severity(severityLevel)
                .status(AlarmStatus.ACTIVE)
                .message(String.format("Hata Analizi (Grup ID: %d)\nKök Neden: %s\nÖneri: %s\nGüven Skoru: %%%.0f",
                        analysis.getLogGroupId(),
                        analysis.getRootCause(),
                        analysis.getSuggestion(),
                        analysis.getConfidenceScore() != null ? analysis.getConfidenceScore() * 100 : 0.0))
                .triggeredAt(Instant.now())
                .build();

        alarmRepository.save(alarm);
        notifier.notifyAll(TITLE, alarm.getMessage());
    }
}
