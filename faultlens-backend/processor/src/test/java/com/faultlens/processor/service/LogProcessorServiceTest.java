package com.faultlens.processor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.faultlens.common.dto.LogEntryDto;
import com.faultlens.common.dto.LogEventDto;
import com.faultlens.common.enums.Severity;
import com.faultlens.processor.classifier.SeverityClassifier;
import com.faultlens.processor.parser.PatternRegistry;
import com.faultlens.processor.event.LogProcessedEvent;
import org.springframework.context.ApplicationEventPublisher;
import com.faultlens.processor.model.LogEntry;
import com.faultlens.processor.parser.LogParser;
import com.faultlens.processor.parser.ParsedLog;
import com.faultlens.processor.repository.LogEntryRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LogProcessorServiceTest {
    private PatternRegistry patternRegistry;
    private SeverityClassifier severityClassifier;
    private LogGroupingService groupingService;
    private LogEntryRepository entryRepository;
    private ApplicationEventPublisher eventPublisher;
    private org.springframework.kafka.core.KafkaTemplate<String, LogEventDto> dlqTemplate;
    private LogProcessorService service;

    @BeforeEach
    void setUp() {
        patternRegistry = mock(PatternRegistry.class);
        severityClassifier = mock(SeverityClassifier.class);
        groupingService = mock(LogGroupingService.class);
        entryRepository = mock(LogEntryRepository.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        dlqTemplate = mock(org.springframework.kafka.core.KafkaTemplate.class);

        service = new LogProcessorService(
                patternRegistry,
                severityClassifier,
                groupingService,
                entryRepository,
                eventPublisher,
                dlqTemplate
        );
    }

    @Test
    void processSuccessfullyParsesGroupsPersistsAndPublishes() {
        LogEventDto event = new LogEventDto();
        event.setMessage("Test message");
        event.setTimestamp(Instant.now());
        event.setLabels(Map.of("sourceId", "123"));

        LogParser mockParser = mock(LogParser.class);
        ParsedLog parsedLog = new ParsedLog("Test message", null, Severity.ERROR, Map.of());
        when(patternRegistry.selectParser(event)).thenReturn(mockParser);
        when(mockParser.parse(event)).thenReturn(parsedLog);

        when(severityClassifier.classify(eq(event), eq(parsedLog))).thenReturn(Severity.ERROR);

        when(groupingService.fingerprint(any(), any(), any())).thenReturn("fp");
        when(groupingService.groupAll(anyCollection())).thenReturn(new LogGroupingService.GroupingResult(Map.of("fp", 456L), java.util.Set.of()));

        LogEntry entry = LogEntry.builder()
                .id(789L)
                .severity(Severity.ERROR)
                .message("Test message")
                .sourceId(123L)
                .groupId(456L)
                .build();
        when(entryRepository.saveAll(anyList())).thenReturn(List.of(entry));

        LogEntryDto result = service.process(event);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(789L);
        assertThat(result.getSeverity()).isEqualTo(Severity.ERROR);
        assertThat(result.getGroupId()).isEqualTo(456L);

        verify(eventPublisher).publishEvent(any(LogProcessedEvent.class));
        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<List<LogEntry>> saved = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(entryRepository).saveAll(saved.capture());
        // the group id from the batch upsert is put on the entry before it is stored
        assertThat(saved.getValue()).extracting(LogEntry::getGroupId).containsExactly(456L);
    }

    @Test
    void processInfoLogDoesNotSendToKafkaErrorTopic() {
        LogEventDto event = new LogEventDto();
        event.setMessage("Info level message");

        LogParser mockParser = mock(LogParser.class);
        ParsedLog parsedLog = new ParsedLog("Info level message", null, Severity.INFO, Map.of());
        when(patternRegistry.selectParser(event)).thenReturn(mockParser);
        when(mockParser.parse(event)).thenReturn(parsedLog);

        when(severityClassifier.classify(eq(event), eq(parsedLog))).thenReturn(Severity.INFO);

        when(groupingService.fingerprint(any(), any(), any())).thenReturn("fp");
        when(groupingService.groupAll(anyCollection())).thenReturn(new LogGroupingService.GroupingResult(Map.of("fp", 456L), java.util.Set.of()));

        LogEntry entry = LogEntry.builder()
                .id(789L)
                .severity(Severity.INFO)
                .message("Info level message")
                .groupId(456L)
                .build();
        when(entryRepository.saveAll(anyList())).thenReturn(List.of(entry));

        LogEntryDto result = service.process(event);

        assertThat(result.getSeverity()).isEqualTo(Severity.INFO);
        verify(eventPublisher).publishEvent(any(LogProcessedEvent.class));
    }

    @Test
    void errorsOfIgnoredGroupsAreNotSentForAnalysis() {
        LogEventDto event = new LogEventDto();
        event.setMessage("boom");

        LogParser mockParser = mock(LogParser.class);
        ParsedLog parsedLog = new ParsedLog("boom", null, Severity.ERROR, Map.of());
        when(patternRegistry.selectParser(event)).thenReturn(mockParser);
        when(mockParser.parse(event)).thenReturn(parsedLog);
        when(severityClassifier.classify(eq(event), eq(parsedLog))).thenReturn(Severity.ERROR);
        when(groupingService.fingerprint(any(), any(), any())).thenReturn("fp");
        // Group 456 is IGNORED: the analyzer would raise an alarm for it (AnalysisEventListener).
        when(groupingService.groupAll(anyCollection()))
                .thenReturn(new LogGroupingService.GroupingResult(Map.of("fp", 456L), java.util.Set.of(456L)));
        when(entryRepository.saveAll(anyList())).thenReturn(List.of(LogEntry.builder()
                .id(789L).severity(Severity.ERROR).message("boom").groupId(456L).build()));

        service.process(event);

        org.mockito.ArgumentCaptor<LogProcessedEvent> published = org.mockito.ArgumentCaptor.forClass(LogProcessedEvent.class);
        verify(eventPublisher).publishEvent(published.capture());
        assertThat(published.getValue().toErrorQueue()).isEmpty();
        assertThat(published.getValue().toPublish()).hasSize(1);
        assertThat(published.getValue().mutedGroupIds()).containsExactly(456L);
    }
}
