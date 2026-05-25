package com.loglens.processor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.loglens.common.dto.LogEntryDto;
import com.loglens.common.dto.LogEventDto;
import com.loglens.common.enums.Severity;
import com.loglens.processor.classifier.SeverityClassifier;
import com.loglens.processor.kafka.LogErrorProducer;
import com.loglens.processor.model.LogEntry;
import com.loglens.processor.model.LogGroup;
import com.loglens.processor.parser.LogParser;
import com.loglens.processor.parser.ParsedLog;
import com.loglens.processor.parser.PatternRegistry;
import com.loglens.processor.realtime.RealtimeLogPublisher;
import com.loglens.processor.repository.LogEntryRepository;
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
    private LogErrorProducer errorProducer;
    private RealtimeLogPublisher realtimeLogPublisher;
    private org.springframework.kafka.core.KafkaTemplate<String, LogEventDto> dlqTemplate;
    private LogProcessorService service;

    @BeforeEach
    void setUp() {
        patternRegistry = mock(PatternRegistry.class);
        severityClassifier = mock(SeverityClassifier.class);
        groupingService = mock(LogGroupingService.class);
        entryRepository = mock(LogEntryRepository.class);
        errorProducer = mock(LogErrorProducer.class);
        realtimeLogPublisher = mock(RealtimeLogPublisher.class);
        dlqTemplate = mock(org.springframework.kafka.core.KafkaTemplate.class);

        service = new LogProcessorService(
                patternRegistry,
                severityClassifier,
                groupingService,
                entryRepository,
                errorProducer,
                realtimeLogPublisher,
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

        LogGroup mockGroup = new LogGroup();
        mockGroup.setId(456L);
        when(groupingService.group(any(), any(), any(), any(), any())).thenReturn(mockGroup);

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

        verify(errorProducer).send(any(LogEntryDto.class));
        verify(realtimeLogPublisher).publish(any(LogEntryDto.class));
        verify(entryRepository).saveAll(anyList());
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

        LogGroup mockGroup = new LogGroup();
        mockGroup.setId(456L);
        when(groupingService.group(any(), any(), any(), any(), any())).thenReturn(mockGroup);

        LogEntry entry = LogEntry.builder()
                .id(789L)
                .severity(Severity.INFO)
                .message("Info level message")
                .groupId(456L)
                .build();
        when(entryRepository.saveAll(anyList())).thenReturn(List.of(entry));

        LogEntryDto result = service.process(event);

        assertThat(result.getSeverity()).isEqualTo(Severity.INFO);
        verify(errorProducer, never()).send(any());
        verify(realtimeLogPublisher).publish(any());
    }
}
