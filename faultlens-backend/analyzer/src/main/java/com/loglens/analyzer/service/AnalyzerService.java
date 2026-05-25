package com.loglens.analyzer.service;

import com.loglens.analyzer.correlation.DeploymentCorrelator;
import com.loglens.analyzer.correlation.DeploymentCorrelator.DeploymentCorrelation;
import com.loglens.analyzer.engine.AiAnalysisEngine;
import com.loglens.analyzer.engine.AnalysisResult;
import com.loglens.analyzer.engine.RuleBasedEngine;
import com.loglens.analyzer.model.Analysis;
import com.loglens.analyzer.repository.AnalysisRepository;
import com.loglens.common.dto.AnalysisResultDto;
import com.loglens.common.dto.LogEntryDto;
import com.loglens.common.exception.ResourceNotFoundException;
import com.loglens.processor.model.LogGroup;
import com.loglens.processor.repository.LogGroupRepository;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AnalyzerService {
    private final RuleBasedEngine ruleBasedEngine;
    private final Optional<AiAnalysisEngine> aiAnalysisEngine;
    private final DeploymentCorrelator deploymentCorrelator;
    private final AnalysisRepository analysisRepository;
    private final LogGroupRepository logGroupRepository;
    private final KafkaTemplate<String, AnalysisResultDto> analysisKafkaTemplate;

    /**
     * Analyzes a log entry, persists the result and publishes it.
     */
    public AnalysisResultDto analyze(LogEntryDto entry) {
        LogGroup group = logGroupRepository.findById(entry.getGroupId())
                .orElseThrow(() -> new ResourceNotFoundException("Log group not found: " + entry.getGroupId()));
        AnalysisResult result = selectBestResult(
                ruleBasedEngine.analyze(entry, group),
                aiAnalysisEngine
                        .filter(AiAnalysisEngine::isAvailable)
                        .map(engine -> engine.analyze(entry, group))
                        .orElse(null));
        result = applyDeploymentCorrelation(entry, result);
        Analysis saved = analysisRepository.save(Analysis.builder()
                .logGroupId(group.getId())
                .logEntryId(entry.getId())
                .rootCause(result.rootCause())
                .suggestion(result.suggestion())
                .affectedDeployment(result.affectedDeployment())
                .confidenceScore(result.confidenceScore())
                .engineType(result.engineType())
                .analyzedAt(Instant.now())
                .build());
        AnalysisResultDto dto = toDto(saved);
        analysisKafkaTemplate.send("log-analysis", String.valueOf(dto.getLogGroupId()), dto);
        return dto;
    }

    /**
     * Triggers analysis for a group without a concrete log entry.
     */
    public AnalysisResultDto analyzeGroup(Long groupId) {
        LogGroup group = logGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Log group not found: " + groupId));
        LogEntryDto entry = LogEntryDto.builder()
                .groupId(groupId)
                .message(group.getFirstMessage())
                .severity(group.getSeverity())
                .timestamp(group.getLastSeenAt())
                .build();
        return analyze(entry);
    }

    /**
     * Converts analysis entity to DTO.
     */
    public AnalysisResultDto toDto(Analysis analysis) {
        return AnalysisResultDto.builder()
                .id(analysis.getId())
                .logGroupId(analysis.getLogGroupId())
                .logEntryId(analysis.getLogEntryId())
                .rootCause(analysis.getRootCause())
                .suggestion(analysis.getSuggestion())
                .affectedDeployment(analysis.getAffectedDeployment())
                .confidenceScore(analysis.getConfidenceScore())
                .engineType(analysis.getEngineType())
                .analyzedAt(analysis.getAnalyzedAt())
                .build();
    }

    private AnalysisResult selectBestResult(AnalysisResult ruleBased, AnalysisResult ai) {
        if (ai == null || ai.confidenceScore() == null) {
            return ruleBased;
        }
        double ruleConfidence = ruleBased.confidenceScore() == null ? 0.0 : ruleBased.confidenceScore();
        return ai.confidenceScore() > ruleConfidence ? ai : ruleBased;
    }

    private AnalysisResult applyDeploymentCorrelation(LogEntryDto entry, AnalysisResult result) {
        return deploymentCorrelator.correlate(entry)
                .map(correlation -> withCorrelation(result, correlation))
                .orElse(result);
    }

    private AnalysisResult withCorrelation(AnalysisResult result, DeploymentCorrelation correlation) {
        double confidence = result.confidenceScore() == null ? 0.0 : result.confidenceScore();
        double adjustedConfidence = Math.max(0.0, confidence - correlation.confidencePenalty());
        return result.withCorrelation(correlation.affectedDeployment(), adjustedConfidence);
    }
}
