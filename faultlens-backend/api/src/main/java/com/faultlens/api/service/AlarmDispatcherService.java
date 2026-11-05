package com.faultlens.api.service;

import com.faultlens.api.model.Alarm;
import com.faultlens.api.model.AlarmChannel;
import com.faultlens.api.model.AlarmRule;
import com.faultlens.api.model.AlarmStatus;
import com.faultlens.api.repository.AlarmRepository;
import com.faultlens.common.dto.AnalysisResultDto;
import com.faultlens.common.enums.Severity;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Turns an analysis into an alarm: stores it (synchronously, so it is never lost) and hands the
 * notifications to {@link AlarmNotifier}, which sends them asynchronously.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlarmDispatcherService {
    static final String TITLE = "Yeni Kritik Hata Tespit Edildi!";
    /** alarms.rule_id of rule alarms ("RULE:<id>"); built-in alarms use e.g. AI_ANALYSIS. */
    static final String RULE_PREFIX = "RULE:";

    private final AlarmRepository alarmRepository;
    private final AlarmNotifier notifier;

    public void dispatchAlarm(AnalysisResultDto analysis) {
        log.info("Dispatching alarm for analysis ID: {}", analysis.getId());

        Severity severityLevel = analysis.getConfidenceScore() != null && analysis.getConfidenceScore() > 0.8
                ? Severity.CRITICAL : Severity.ERROR;

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

    /**
     * Stores an alarm raised by an operator-defined rule and notifies the rule's channels.
     *
     * <p>REQUIRES_NEW: the rule evaluator runs in an AFTER_COMMIT listener, where the processor's
     * transaction is already committed but still bound; joining it would silently not persist.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Alarm raiseForRule(AlarmRule rule, Severity severity, Long sourceId, String message) {
        log.info("Alarm rule {} ({}) fired", rule.getId(), rule.getName());
        Alarm alarm = alarmRepository.save(Alarm.builder()
                .ruleId(RULE_PREFIX + rule.getId())
                .alarmRuleId(rule.getId())
                .ruleName(rule.getName())
                .sourceId(sourceId)
                .severity(severity == null ? rule.getSeverity() : severity)
                .status(AlarmStatus.ACTIVE)
                .message(message)
                .triggeredAt(Instant.now())
                .build());
        notifier.notifyAll("Alarm: " + rule.getName(), message, java.util.EnumSet.copyOf(
                rule.getChannels().isEmpty() ? java.util.List.of(AlarmChannel.IN_APP) : rule.getChannels()));
        return alarm;
    }
}
