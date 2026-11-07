package com.faultlens.api.ingest;

import com.faultlens.common.enums.Severity;
import java.time.Instant;
import java.util.Map;

/**
 * One log record pushed by an application, after decoding (JSON or OTLP) and before it becomes a
 * {@code log-raw} event. Every field except {@code message} is optional.
 *
 * @param severity null lets the processor classify the line (parsers, stack traces)
 */
public record PushedLog(Instant timestamp, Severity severity, String message, String stackTrace,
                        String serviceName, String namespace, String podName, String containerName,
                        String cluster, Map<String, String> attributes) {
}
