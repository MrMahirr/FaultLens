package com.faultlens.api.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.faultlens.analyzer.model.Analysis;
import com.faultlens.analyzer.repository.AnalysisRepository;
import com.faultlens.api.model.Alarm;
import com.faultlens.api.model.AlarmStatus;
import com.faultlens.api.repository.AlarmRepository;
import com.faultlens.collector.kafka.LogEventProducer;
import com.faultlens.common.dto.LogEventDto;
import com.faultlens.common.enums.Severity;
import com.faultlens.processor.model.LogEntry;
import com.faultlens.processor.model.LogGroup;
import com.faultlens.processor.repository.LogEntryRepository;
import com.faultlens.processor.repository.LogGroupRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * End-to-end pipeline: collector producer → {@code log-raw} → processor (entry + group) →
 * {@code log-errors} → analyzer (rule engine) → {@code log-analysis} → alarm.
 *
 * <p>Locks the Kafka (de)serialization contract of every hop, which P05's Jackson 3 migration
 * can silently break.
 */
class PipelineIT extends AbstractIntegrationTest {
    private static final Duration PIPELINE_TIMEOUT = Duration.ofSeconds(60);

    @Autowired
    private LogEventProducer producer;
    @Autowired
    private LogEntryRepository entryRepository;
    @Autowired
    private LogGroupRepository groupRepository;
    @Autowired
    private AnalysisRepository analysisRepository;
    @Autowired
    private AlarmRepository alarmRepository;

    @Test
    void javaStackTraceFlowsFromRawTopicToAlarm() throws Exception {
        // Unique frame → unique fingerprint → fresh group (the analyzer throttles a group to 1/hour).
        String marker = "PipelineIT" + UUID.randomUUID().toString().replace("-", "");
        String trace = """
                java.lang.NullPointerException: Cannot invoke "String.length()" because "name" is null
                \tat com.example.orders.%s.handle(OrderService.java:42)
                \tat com.example.orders.OrderController.create(OrderController.java:17)""".formatted(marker);

        producer.send(LogEventDto.builder()
                .id(UUID.randomUUID().toString())
                .source("it")
                .namespace("it")
                .podName("orders-" + marker)
                .serviceName("orders")
                .severity(Severity.ERROR)
                .message(trace)
                .rawLine(trace)
                .timestamp(Instant.now())
                .labels(Map.of("stream", "stderr"))
                .build()).get(10, TimeUnit.SECONDS);

        // 1. processor: entry + group persisted
        LogEntry entry = await().atMost(PIPELINE_TIMEOUT).until(
                () -> entryRepository.findAll().stream()
                        .filter(e -> e.getStackTrace() != null && e.getStackTrace().contains(marker))
                        .findFirst().orElse(null),
                found -> found != null);
        assertThat(entry.getSeverity()).isEqualTo(Severity.ERROR);
        assertThat(entry.getMessage()).startsWith("java.lang.NullPointerException");
        assertThat(entry.getPodName()).isEqualTo("orders-" + marker);
        assertThat(entry.getGroupId()).isNotNull();

        LogGroup group = groupRepository.findById(entry.getGroupId()).orElseThrow();
        assertThat(group.getCount()).isEqualTo(1L);
        assertThat(group.getSeverity()).isEqualTo(Severity.ERROR);

        // 2. analyzer: rule-based NPE analysis for this group
        Analysis analysis = await().atMost(PIPELINE_TIMEOUT).until(
                () -> analysisRepository.findFirstByLogGroupIdOrderByAnalyzedAtDesc(group.getId()).orElse(null),
                found -> found != null);
        assertThat(analysis.getEngineType()).isEqualTo("RULE_BASED");
        assertThat(analysis.getLogEntryId()).isEqualTo(entry.getId());
        assertThat(analysis.getRootCause()).startsWith("Null referans");
        assertThat(analysis.getSuggestion()).contains(marker);
        assertThat(analysis.getConfidenceScore()).isEqualTo(0.9);

        // 3. api: alarm raised from the log-analysis event (confidence 0.9 > 0.8 → CRITICAL)
        String groupTag = "Grup ID: " + group.getId() + ")";
        Alarm alarm = await().atMost(PIPELINE_TIMEOUT).until(
                () -> alarmRepository.findAll().stream()
                        .filter(a -> a.getMessage() != null && a.getMessage().contains(groupTag))
                        .findFirst().orElse(null),
                found -> found != null);
        assertThat(alarm.getStatus()).isEqualTo(AlarmStatus.ACTIVE);
        assertThat(alarm.getSeverity()).isEqualTo("CRITICAL");
        assertThat(alarm.getMessage()).contains("Null referans");
    }
}
