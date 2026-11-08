package com.faultlens.api.ingest;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * One record of {@code POST /api/v1/ingest/logs}, for the OpenAPI contract only: the endpoint
 * reads the raw body with {@link JsonLogDecoder} (which also accepts camelCase names, epoch
 * milliseconds and common severity spellings), so this type documents the canonical form.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "One log record. Only `message` is required.", requiredProperties = "message")
public record IngestLogRequest(
        @Schema(description = "ISO-8601 instant or epoch milliseconds; default: time of receipt",
                example = "2026-11-07T10:00:00Z")
        String timestamp,
        @Schema(description = "TRACE, DEBUG, INFO, WARN, ERROR, CRITICAL (also: warning, fatal, err…); "
                + "absent: classified by the processor", example = "ERROR")
        String severity,
        @Schema(description = "The log line", example = "PaymentGatewayTimeoutException: stripe-proxy 3000ms")
        String message,
        String stackTrace,
        String serviceName,
        String namespace,
        String podName,
        String containerName,
        String cluster,
        Map<String, String> attributes) {
}
