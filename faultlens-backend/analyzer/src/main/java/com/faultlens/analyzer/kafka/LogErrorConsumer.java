package com.faultlens.analyzer.kafka;

import com.faultlens.analyzer.service.AnalyzerService;
import com.faultlens.common.dto.LogEntryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LogErrorConsumer {
    private final AnalyzerService analyzerService;

    /**
     * Consumes error logs and triggers analysis.
     */
    @KafkaListener(topics = "log-errors", groupId = "faultlens-analyzer", containerFactory = "logEntryKafkaListenerContainerFactory")
    public void consume(LogEntryDto entry) {
        analyzerService.analyze(entry);
    }
}
