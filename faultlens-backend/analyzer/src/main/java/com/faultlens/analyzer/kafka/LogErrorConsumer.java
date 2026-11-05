package com.faultlens.analyzer.kafka;

import com.faultlens.analyzer.model.Analysis;
import com.faultlens.analyzer.repository.AnalysisRepository;
import com.faultlens.analyzer.service.AnalyzerService;
import com.faultlens.common.dto.LogEntryDto;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogErrorConsumer {
    private final AnalyzerService analyzerService;
    private final AnalysisRepository analysisRepository;
    private final AnalysisThrottle throttle;

    /**
     * Consumes error logs and triggers analysis, at most once per group per hour.
     */
    @KafkaListener(topics = "log-errors", groupId = "faultlens-analyzer", containerFactory = "logEntryKafkaListenerContainerFactory")
    public void consume(LogEntryDto entry) {
        Long groupId = entry.getGroupId();
        if (groupId != null) {
            if (analysedWithinWindow(groupId)) {
                return;
            }
            // Atomic claim: the database check above alone lets concurrent consumers both pass.
            if (!throttle.tryAcquire(groupId)) {
                log.debug("Group {} is being or was just analysed by another consumer", groupId);
                return;
            }
        }
        analyzerService.analyze(entry);
    }

    private boolean analysedWithinWindow(Long groupId) {
        Optional<Analysis> lastAnalysis = analysisRepository.findFirstByLogGroupIdOrderByAnalyzedAtDesc(groupId);
        Instant windowStart = Instant.now().minus(AnalysisThrottle.WINDOW);
        return lastAnalysis.isPresent() && lastAnalysis.get().getAnalyzedAt().isAfter(windowStart);
    }
}
