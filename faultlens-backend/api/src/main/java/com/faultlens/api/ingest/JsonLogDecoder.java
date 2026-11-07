package com.faultlens.api.ingest;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Decodes the simple JSON format of {@code POST /api/v1/ingest/logs}: an array of records, or
 * {@code {"logs": [...]}}, or a single record object. Fields are snake_case (camelCase is
 * accepted too):
 *
 * <pre>{"timestamp": "2026-11-07T10:00:00Z", "severity": "ERROR", "message": "…",
 *  "stack_trace": "…", "service_name": "orders", "namespace": "prod", "pod_name": "…",
 *  "container_name": "…", "cluster": "…", "attributes": {"k": "v"}}</pre>
 *
 * <p>{@code timestamp} may also be epoch milliseconds; a record is rejected if it is not an
 * object or has no {@code message} (checked by {@link IngestService}).
 */
@Component
public class JsonLogDecoder {
    private final ObjectMapper objectMapper;

    public JsonLogDecoder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<PushedLog> decode(byte[] body) {
        JsonNode root;
        try {
            root = objectMapper.readTree(body);
        } catch (JacksonException ex) {
            throw new IngestRejectedException(HttpStatus.BAD_REQUEST, "INVALID_JSON", "Body is not valid JSON");
        }
        JsonNode records = root != null && root.isObject() && root.has("logs") ? root.get("logs") : root;
        if (records == null || records.isNull() || records.isMissingNode()) {
            return List.of();
        }
        if (records.isObject()) {
            return List.of(record(records, 0));
        }
        if (!records.isArray()) {
            throw new IngestRejectedException(HttpStatus.BAD_REQUEST, "INVALID_JSON",
                    "Expected an array of log records or {\"logs\": [...]}");
        }
        List<PushedLog> out = new ArrayList<>(records.size());
        for (int i = 0; i < records.size(); i++) {
            out.add(record(records.get(i), i));
        }
        return out;
    }

    private static PushedLog record(JsonNode node, int index) {
        if (node == null || !node.isObject()) {
            throw new IngestRejectedException(HttpStatus.BAD_REQUEST, "INVALID_LOG_RECORD",
                    "Record " + index + " is not an object");
        }
        return new PushedLog(
                timestamp(node.get("timestamp"), index),
                IngestService.parseSeverity(text(node, "severity", "level")),
                text(node, "message", "msg"),
                text(node, "stack_trace", "stackTrace"),
                text(node, "service_name", "serviceName"),
                text(node, "namespace"),
                text(node, "pod_name", "podName"),
                text(node, "container_name", "containerName"),
                text(node, "cluster"),
                attributes(node.get("attributes")));
    }

    private static Instant timestamp(JsonNode value, int index) {
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isNumber()) {
            return Instant.ofEpochMilli(value.asLong());
        }
        try {
            return Instant.parse(value.asString());
        } catch (DateTimeParseException ex) {
            throw new IngestRejectedException(HttpStatus.BAD_REQUEST, "INVALID_LOG_RECORD",
                    "Record " + index + ": timestamp must be ISO-8601 or epoch milliseconds");
        }
    }

    private static String text(JsonNode node, String... names) {
        for (String name : names) {
            JsonNode value = node.get(name);
            if (value != null && !value.isNull()) {
                return value.isValueNode() ? value.asString() : value.toString();
            }
        }
        return null;
    }

    private static Map<String, String> attributes(JsonNode node) {
        if (node == null || !node.isObject()) {
            return Map.of();
        }
        Map<String, String> out = new LinkedHashMap<>();
        for (Map.Entry<String, JsonNode> entry : node.properties()) {
            JsonNode value = entry.getValue();
            if (value != null && !value.isNull()) {
                out.put(entry.getKey(), value.isValueNode() ? value.asString() : value.toString());
            }
        }
        return out;
    }
}
