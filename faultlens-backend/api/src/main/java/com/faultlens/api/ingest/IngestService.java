package com.faultlens.api.ingest;

import com.faultlens.collector.kafka.LogEventProducer;
import com.faultlens.collector.model.LogSource;
import com.faultlens.collector.repository.LogSourceRepository;
import com.faultlens.collector.service.CollectorOrchestrator;
import com.faultlens.common.dto.LogEventDto;
import com.faultlens.common.enums.LogSourceType;
import com.faultlens.common.enums.Severity;
import io.micrometer.core.instrument.Metrics;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Turns pushed log records into {@code log-raw} events (P10 / 1.2), the same topic the collector
 * writes to, so parsing, grouping, alarms and analysis work unchanged.
 *
 * <p>A request is all or nothing: it is checked as a whole (source, record count, message size)
 * and answered only after Kafka acknowledged every record; if that does not happen in time the
 * client gets 503 + Retry-After and should resend (duplicates are possible on a retry, losses
 * are not).
 */
@Slf4j
@Service
public class IngestService {
    /** Attributes copied into the event labels at most (the rest is dropped). */
    static final int MAX_ATTRIBUTES = 32;
    static final int MAX_ATTRIBUTE_VALUE = 1_000;

    private final LogEventProducer producer;
    private final LogSourceRepository sources;
    private final CollectorOrchestrator orchestrator;
    private final int maxRecords;
    private final int maxMessageChars;
    private final long kafkaTimeoutMs;

    public IngestService(LogEventProducer producer, LogSourceRepository sources, CollectorOrchestrator orchestrator,
                         @Value("${faultlens.ingest.max-records:5000}") int maxRecords,
                         @Value("${faultlens.ingest.max-message-chars:65536}") int maxMessageChars,
                         @Value("${faultlens.ingest.kafka-timeout-ms:5000}") long kafkaTimeoutMs) {
        this.producer = producer;
        this.sources = sources;
        this.orchestrator = orchestrator;
        this.maxRecords = maxRecords;
        this.maxMessageChars = maxMessageChars;
        this.kafkaTimeoutMs = kafkaTimeoutMs;
    }

    /**
     * Publishes the records of one request under the key's source.
     *
     * @param format "json" or "otlp" (metrics label)
     * @return number of records accepted
     */
    public int ingest(Long sourceId, List<PushedLog> records, String format) {
        LogSource source = requirePushSource(sourceId);
        if (records.isEmpty()) {
            return 0;
        }
        if (records.size() > maxRecords) {
            reject(sourceId, format, records.size());
            throw new IngestRejectedException(HttpStatus.CONTENT_TOO_LARGE, "TOO_MANY_RECORDS",
                    "At most " + maxRecords + " records per request (got " + records.size() + ")");
        }

        List<LogEventDto> events = new ArrayList<>(records.size());
        for (int i = 0; i < records.size(); i++) {
            PushedLog record = records.get(i);
            if (record.message() == null || record.message().isBlank()) {
                reject(sourceId, format, records.size());
                throw new IngestRejectedException(HttpStatus.BAD_REQUEST, "INVALID_LOG_RECORD",
                        "Record " + i + " has no message");
            }
            events.add(toEvent(source, record, format));
        }

        try {
            CompletableFuture.allOf(events.stream().map(producer::send).toArray(CompletableFuture[]::new))
                    .get(kafkaTimeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw unavailable(sourceId, format, records.size(), ex);
        } catch (Exception ex) {
            throw unavailable(sourceId, format, records.size(), ex);
        }

        orchestrator.recordPushed(sourceId);
        Metrics.counter("faultlens.ingest.records", "source", sourceId.toString(), "format", format,
                "result", "accepted").increment(records.size());
        return records.size();
    }

    private LogSource requirePushSource(Long sourceId) {
        if (sourceId == null) {
            throw new IngestRejectedException(HttpStatus.FORBIDDEN, "API_KEY_NO_SOURCE",
                    "This API key is not bound to an HTTP source (it may have been deleted)");
        }
        LogSource source = sources.findById(sourceId).orElseThrow(() -> new IngestRejectedException(
                HttpStatus.FORBIDDEN, "API_KEY_NO_SOURCE", "The source of this API key no longer exists"));
        if (source.getType() != LogSourceType.HTTP) {
            throw new IngestRejectedException(HttpStatus.FORBIDDEN, "API_KEY_NO_SOURCE",
                    "The source of this API key is not an HTTP source");
        }
        if (!source.isEnabled()) {
            throw new IngestRejectedException(HttpStatus.CONFLICT, "SOURCE_DISABLED",
                    "The source is disabled; enable it to accept logs");
        }
        return source;
    }

    LogEventDto toEvent(LogSource source, PushedLog record, String format) {
        String message = truncate(record.message().strip(), maxMessageChars);
        String stackTrace = record.stackTrace() == null || record.stackTrace().isBlank()
                ? null : truncate(record.stackTrace().strip(), maxMessageChars);
        // The processor finds stack traces in the message text (JavaStackTraceParser).
        String text = stackTrace == null ? message : message + "\n" + stackTrace;

        Map<String, String> labels = new HashMap<>();
        if (record.attributes() != null) {
            record.attributes().entrySet().stream()
                    .filter(e -> e.getKey() != null && e.getValue() != null)
                    .limit(MAX_ATTRIBUTES)
                    .forEach(e -> labels.put(e.getKey(), truncate(e.getValue(), MAX_ATTRIBUTE_VALUE)));
        }
        // Set last: the client cannot pick another source or fake a stream.
        labels.remove("stream");
        labels.put("sourceId", source.getId().toString());
        labels.put("ingest", format);

        return LogEventDto.builder()
                .id(UUID.randomUUID().toString())
                .source(source.getName())
                .timestamp(record.timestamp() == null ? Instant.now() : record.timestamp())
                .severity(record.severity())
                .message(text)
                .rawLine(text)
                .serviceName(record.serviceName())
                .namespace(record.namespace())
                .podName(record.podName())
                .containerName(record.containerName())
                .cluster(record.cluster())
                .labels(labels)
                .build();
    }

    /** Maps common severity names (also syslog / log4j spellings); null if unknown. */
    public static Severity parseSeverity(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return switch (value.trim().toUpperCase(Locale.ROOT)) {
            case "TRACE", "FINEST", "FINER" -> Severity.TRACE;
            case "DEBUG", "FINE", "CONFIG" -> Severity.DEBUG;
            case "INFO", "INFORMATION", "NOTICE" -> Severity.INFO;
            case "WARN", "WARNING" -> Severity.WARN;
            case "ERROR", "ERR", "SEVERE" -> Severity.ERROR;
            case "CRITICAL", "CRIT", "FATAL", "ALERT", "EMERG", "EMERGENCY" -> Severity.CRITICAL;
            default -> null;
        };
    }

    private IngestRejectedException unavailable(Long sourceId, String format, int count, Exception cause) {
        log.warn("Ingest for source {} could not be written to Kafka in {} ms", sourceId, kafkaTimeoutMs, cause);
        reject(sourceId, format, count);
        return new IngestRejectedException(HttpStatus.SERVICE_UNAVAILABLE, "INGEST_UNAVAILABLE",
                "Logs could not be queued right now; retry later", 5L, cause);
    }

    private static void reject(Long sourceId, String format, int count) {
        Metrics.counter("faultlens.ingest.records", "source", String.valueOf(sourceId), "format", format,
                "result", "rejected").increment(count);
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max) + "…";
    }
}
