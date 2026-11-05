package com.faultlens.api.kafka;

import com.faultlens.api.service.AlarmDispatcherService;
import com.faultlens.common.dto.AnalysisResultDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AnalysisEventListener {

    private final AlarmDispatcherService alarmDispatcherService;
    private final AlarmCooldown cooldown;

    public AnalysisEventListener(@Lazy AlarmDispatcherService alarmDispatcherService, AlarmCooldown cooldown) {
        this.alarmDispatcherService = alarmDispatcherService;
        this.cooldown = cooldown;
    }

    /**
     * Raises an alarm for each analysis (the analyzer only analyses ERROR/CRITICAL entries),
     * at most once per log group per cooldown window.
     */
    @KafkaListener(topics = "log-analysis", groupId = "faultlens-api-alarms", containerFactory = "analysisKafkaListenerContainerFactory")
    public void consume(AnalysisResultDto analysis) {
        log.debug("Received analysis result from Kafka: {}", analysis.getId());
        if (!cooldown.tryAcquire(analysis.getLogGroupId())) {
            log.debug("Alarm for group {} suppressed: cooldown active", analysis.getLogGroupId());
            return;
        }
        alarmDispatcherService.dispatchAlarm(analysis);
    }
}
