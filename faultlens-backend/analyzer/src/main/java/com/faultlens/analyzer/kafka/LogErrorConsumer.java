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
    private final com.faultlens.analyzer.repository.AnalysisRepository analysisRepository;

    /**
     * Consumes error logs and triggers analysis.
     */
    @KafkaListener(topics = "log-errors", groupId = "faultlens-analyzer", containerFactory = "logEntryKafkaListenerContainerFactory")
    public void consume(LogEntryDto entry) {
        if (entry.getGroupId() != null) {
            java.util.Optional<com.faultlens.analyzer.model.Analysis> lastAnalysis = 
                analysisRepository.findFirstByLogGroupIdOrderByAnalyzedAtDesc(entry.getGroupId());
            
            if (lastAnalysis.isPresent()) {
                java.time.Instant oneHourAgo = java.time.Instant.now().minus(1, java.time.temporal.ChronoUnit.HOURS);
                if (lastAnalysis.get().getAnalyzedAt().isAfter(oneHourAgo)) {
                    // Skip auto-analysis if analyzed within the last hour
                    return;
                }
            }
        }
        
        analyzerService.analyze(entry);
    }
}
