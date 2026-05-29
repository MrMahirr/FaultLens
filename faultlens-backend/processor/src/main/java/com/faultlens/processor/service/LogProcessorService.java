package com.faultlens.processor.service;

import com.faultlens.common.dto.LogEntryDto;
import com.faultlens.common.dto.LogEventDto;
import com.faultlens.common.enums.Severity;
import com.faultlens.processor.classifier.SeverityClassifier;
import com.faultlens.processor.kafka.LogErrorProducer;
import com.faultlens.processor.model.LogEntry;
import com.faultlens.processor.model.LogGroup;
import com.faultlens.processor.parser.ParsedLog;
import com.faultlens.processor.parser.PatternRegistry;
import com.faultlens.processor.realtime.RealtimeLogPublisher;
import com.faultlens.processor.repository.LogEntryRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class LogProcessorService {
    private final PatternRegistry patternRegistry;
    private final SeverityClassifier severityClassifier;
    private final LogGroupingService groupingService;
    private final LogEntryRepository entryRepository;
    private final LogErrorProducer errorProducer;
    private final RealtimeLogPublisher realtimeLogPublisher;

    private final org.springframework.kafka.core.KafkaTemplate<String, LogEventDto> dlqTemplate;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LogProcessorService.class);

    public LogProcessorService(
            PatternRegistry patternRegistry,
            SeverityClassifier severityClassifier,
            LogGroupingService groupingService,
            LogEntryRepository entryRepository,
            LogErrorProducer errorProducer,
            RealtimeLogPublisher realtimeLogPublisher,
            @Qualifier("processorLogEventKafkaTemplate") org.springframework.kafka.core.KafkaTemplate<String, LogEventDto> dlqTemplate) {
        this.patternRegistry = patternRegistry;
        this.severityClassifier = severityClassifier;
        this.groupingService = groupingService;
        this.entryRepository = entryRepository;
        this.errorProducer = errorProducer;
        this.realtimeLogPublisher = realtimeLogPublisher;
        this.dlqTemplate = dlqTemplate;
    }

    /**
     * Parses, groups, persists and forwards a raw log event.
     */
    public LogEntryDto process(LogEventDto event) {
        List<LogEntryDto> results = processBatch(List.of(event));
        return results.isEmpty() ? null : results.getFirst();
    }

    /**
     * Parses, groups, persists and forwards a batch of raw log events.
     */
    public List<LogEntryDto> processBatch(List<LogEventDto> events) {
        List<PendingLogEntry> pendingEntries = new ArrayList<>();
        for (LogEventDto event : events) {
            try {
                pendingEntries.add(toPendingEntry(event));
            } catch (Exception ex) {
                log.error("Failed to process event, sending to DLQ: {}", event, ex);
                dlqTemplate.send("log-raw-dlq", event);
            }
        }
        
        if (pendingEntries.isEmpty()) {
            return List.of();
        }

        List<LogEntry> savedEntries = entryRepository.saveAll(pendingEntries.stream()
                .map(PendingLogEntry::entry)
                .toList());
        List<LogEntryDto> dtos = new ArrayList<>(savedEntries.size());
        List<LogEntryDto> toPublish = new ArrayList<>();
        List<LogEntryDto> toErrorQueue = new ArrayList<>();

        for (int i = 0; i < savedEntries.size(); i++) {
            PendingLogEntry pending = pendingEntries.get(i);
            LogEntryDto dto = toDto(savedEntries.get(i), pending.event(), pending.parsed());
            dtos.add(dto);
            if (dto.getSeverity() == Severity.ERROR || dto.getSeverity() == Severity.CRITICAL) {
                toErrorQueue.add(dto);
            }
            toPublish.add(dto);
        }

        if (org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        toErrorQueue.forEach(errorProducer::send);
                        toPublish.forEach(realtimeLogPublisher::publish);
                    }
                }
            );
        } else {
            toErrorQueue.forEach(errorProducer::send);
            toPublish.forEach(realtimeLogPublisher::publish);
        }

        return dtos;
    }

    private PendingLogEntry toPendingEntry(LogEventDto event) {
        ParsedLog parsed = patternRegistry.selectParser(event).parse(event);
        Severity severity = severityClassifier.classify(event, parsed);
        Instant timestamp = event.getTimestamp() == null ? Instant.now() : event.getTimestamp();
        Long sourceId = extractSourceId(event);
        LogGroup group = groupingService.group(parsed.parsedMessage(), parsed.stackTrace(), severity, sourceId, timestamp);
        LogEntry entry = LogEntry.builder()
                .sourceId(sourceId)
                .groupId(group.getId())
                .severity(severity)
                .message(parsed.parsedMessage() == null ? event.getMessage() : parsed.parsedMessage())
                .parsedMessage(parsed.parsedMessage())
                .stackTrace(parsed.stackTrace())
                .rawLine(event.getRawLine())
                .namespace(event.getNamespace())
                .podName(event.getPodName())
                .containerName(event.getContainerName())
                .serviceName(event.getServiceName())
                .cluster(event.getCluster())
                .timestamp(timestamp)
                .build();
        return new PendingLogEntry(event, parsed, entry);
    }

    /**
     * Converts a stored log entry to DTO.
     */
    public LogEntryDto toDto(LogEntry entry, LogEventDto event, ParsedLog parsed) {
        return LogEntryDto.builder()
                .id(entry.getId())
                .sourceId(entry.getSourceId())
                .groupId(entry.getGroupId())
                .cluster(entry.getCluster())
                .namespace(entry.getNamespace())
                .podName(entry.getPodName())
                .containerName(entry.getContainerName())
                .serviceName(entry.getServiceName())
                .severity(entry.getSeverity())
                .message(entry.getMessage())
                .timestamp(entry.getTimestamp())
                .stackTrace(entry.getStackTrace())
                .parsedMessage(parsed.parsedMessage())
                .build();
    }

    private Long extractSourceId(LogEventDto event) {
        if (event.getLabels() == null || !event.getLabels().containsKey("sourceId")) {
            return null;
        }
        try {
            return Long.valueOf(event.getLabels().get("sourceId"));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private record PendingLogEntry(LogEventDto event, ParsedLog parsed, LogEntry entry) {
    }
}
