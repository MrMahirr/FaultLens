package com.faultlens.api.kafka;

import com.faultlens.common.dto.AnalysisResultDto;
import com.faultlens.api.service.AlarmDispatcherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AnalysisEventListener {

    private final AlarmDispatcherService alarmDispatcherService;

    public AnalysisEventListener(@org.springframework.context.annotation.Lazy AlarmDispatcherService alarmDispatcherService) {
        this.alarmDispatcherService = alarmDispatcherService;
    }

    /**
     * Listens to the log-analysis topic and triggers alarms if the analysis is critical.
     */
    @KafkaListener(topics = "log-analysis", groupId = "faultlens-api-alarms", containerFactory = "analysisKafkaListenerContainerFactory")
    public void consume(AnalysisResultDto analysis) {
        log.debug("Received analysis result from Kafka: {}", analysis.getId());
        
        // Simulating a rule engine: If confidence score is very high, or if it's an AI analysis for an error
        // For demonstration, let's treat every new Analysis as an Alarm trigger if it came from the AI Engine
        // or if it has a high confidence score. We'll just trigger it if the confidence is > 0.5 for now,
        // or since the system analyzes only errors throttled per 1h, ANY analysis is basically an alarm.
        
        // Let's trigger an alarm for every analysis for this feature.
        alarmDispatcherService.dispatchAlarm(analysis);
    }
}
