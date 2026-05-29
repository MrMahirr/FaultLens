package com.faultlens.analyzer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.faultlens.analyzer.correlation.DeploymentCorrelator;
import com.faultlens.analyzer.correlation.DeploymentCorrelator.DeploymentCorrelation;
import com.faultlens.analyzer.engine.AiAnalysisEngine;
import com.faultlens.analyzer.engine.AnalysisResult;
import com.faultlens.analyzer.engine.RuleBasedEngine;
import com.faultlens.analyzer.model.Analysis;
import com.faultlens.analyzer.repository.AnalysisRepository;
import com.faultlens.common.dto.AnalysisResultDto;
import com.faultlens.common.dto.LogEntryDto;
import com.faultlens.common.exception.ResourceNotFoundException;
import com.faultlens.processor.model.LogGroup;
import com.faultlens.processor.repository.LogGroupRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

class AnalyzerServiceTest {
    private RuleBasedEngine ruleBasedEngine;
    private AiAnalysisEngine aiAnalysisEngine;
    private DeploymentCorrelator deploymentCorrelator;
    private AnalysisRepository analysisRepository;
    private LogGroupRepository logGroupRepository;
    private KafkaTemplate<String, AnalysisResultDto> kafkaTemplate;
    private AnalyzerService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        ruleBasedEngine = mock(RuleBasedEngine.class);
        aiAnalysisEngine = mock(AiAnalysisEngine.class);
        deploymentCorrelator = mock(DeploymentCorrelator.class);
        analysisRepository = mock(AnalysisRepository.class);
        logGroupRepository = mock(LogGroupRepository.class);
        kafkaTemplate = mock(KafkaTemplate.class);

        service = new AnalyzerService(
                ruleBasedEngine,
                Optional.of(aiAnalysisEngine),
                deploymentCorrelator,
                analysisRepository,
                logGroupRepository,
                kafkaTemplate
        );
    }

    @Test
    void analyzeThrowsExceptionWhenGroupNotFound() {
        LogEntryDto entry = LogEntryDto.builder().groupId(1L).build();
        when(logGroupRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.analyze(entry))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Log group not found: 1");
    }

    @Test
    void analyzeChoosesBestResultAndSaves() {
        LogEntryDto entry = LogEntryDto.builder()
                .id(100L)
                .groupId(1L)
                .build();

        LogGroup group = new LogGroup();
        group.setId(1L);
        when(logGroupRepository.findById(1L)).thenReturn(Optional.of(group));

        AnalysisResult ruleResult = new AnalysisResult("Rule Cause", "Rule Suggestion", null, 0.8, "RULE");
        AnalysisResult aiResult = new AnalysisResult("AI Cause", "AI Suggestion", null, 0.95, "AI");

        when(ruleBasedEngine.analyze(eq(entry), eq(group))).thenReturn(ruleResult);
        when(aiAnalysisEngine.isAvailable()).thenReturn(true);
        when(aiAnalysisEngine.analyze(eq(entry), eq(group))).thenReturn(aiResult);

        when(deploymentCorrelator.correlate(entry)).thenReturn(Optional.empty());

        Analysis savedAnalysis = Analysis.builder()
                .id(500L)
                .logGroupId(1L)
                .logEntryId(100L)
                .rootCause("AI Cause")
                .suggestion("AI Suggestion")
                .confidenceScore(0.95)
                .engineType("AI")
                .analyzedAt(Instant.now())
                .build();
        when(analysisRepository.save(any(Analysis.class))).thenReturn(savedAnalysis);

        AnalysisResultDto result = service.analyze(entry);

        assertThat(result).isNotNull();
        assertThat(result.getRootCause()).isEqualTo("AI Cause");
        assertThat(result.getConfidenceScore()).isEqualTo(0.95);
        assertThat(result.getEngineType()).isEqualTo("AI");

        verify(analysisRepository).save(any(Analysis.class));
        verify(kafkaTemplate).send(eq("log-analysis"), eq("1"), any(AnalysisResultDto.class));
    }

    @Test
    void analyzeAppliesDeploymentCorrelationConfidencePenalty() {
        LogEntryDto entry = LogEntryDto.builder()
                .id(100L)
                .groupId(1L)
                .build();

        LogGroup group = new LogGroup();
        group.setId(1L);
        when(logGroupRepository.findById(1L)).thenReturn(Optional.of(group));

        AnalysisResult ruleResult = new AnalysisResult("Rule Cause", "Rule Suggestion", null, 0.8, "RULE");

        when(ruleBasedEngine.analyze(eq(entry), eq(group))).thenReturn(ruleResult);
        when(aiAnalysisEngine.isAvailable()).thenReturn(false);

        DeploymentCorrelation correlation = new DeploymentCorrelation("v1.1", 0.3);
        when(deploymentCorrelator.correlate(entry)).thenReturn(Optional.of(correlation));

        Analysis savedAnalysis = Analysis.builder()
                .id(500L)
                .logGroupId(1L)
                .logEntryId(100L)
                .rootCause("Rule Cause")
                .suggestion("Rule Suggestion")
                .affectedDeployment("v1.1")
                .confidenceScore(0.5) // 0.8 - 0.3 = 0.5
                .engineType("RULE")
                .analyzedAt(Instant.now())
                .build();
        when(analysisRepository.save(any(Analysis.class))).thenReturn(savedAnalysis);

        AnalysisResultDto result = service.analyze(entry);

        assertThat(result.getConfidenceScore()).isEqualTo(0.5);
        assertThat(result.getAffectedDeployment()).isEqualTo("v1.1");
    }
}
