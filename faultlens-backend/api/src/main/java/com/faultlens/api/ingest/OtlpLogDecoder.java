package com.faultlens.api.ingest;

import com.faultlens.common.enums.Severity;
import com.google.protobuf.InvalidProtocolBufferException;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.logs.v1.LogRecord;
import io.opentelemetry.proto.logs.v1.ResourceLogs;
import io.opentelemetry.proto.logs.v1.ScopeLogs;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Decodes OTLP/HTTP log exports ({@code ExportLogsServiceRequest}), binary protobuf or the OTLP
 * JSON encoding, into {@link PushedLog}s.
 *
 * <p>Mapping: body → message; {@code exception.stacktrace} → stack trace; resource or record
 * attributes {@code service.name}, {@code k8s.namespace.name}, {@code k8s.pod.name},
 * {@code k8s.container.name} (or {@code container.name}), {@code k8s.cluster.name} → the
 * matching fields; other attributes → labels. Severity from {@code severity_number} (OTLP
 * ranges), else from {@code severity_text}.
 */
@Component
public class OtlpLogDecoder {
    static final String SERVICE = "service.name";
    static final String NAMESPACE = "k8s.namespace.name";
    static final String POD = "k8s.pod.name";
    static final String CONTAINER = "k8s.container.name";
    static final String CONTAINER_ALT = "container.name";
    static final String CLUSTER = "k8s.cluster.name";
    static final String STACKTRACE = "exception.stacktrace";
    static final String EXCEPTION_TYPE = "exception.type";
    static final String EXCEPTION_MESSAGE = "exception.message";

    private final ObjectMapper objectMapper;

    public OtlpLogDecoder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /* ── protobuf ─────────────────────────────────────────────── */

    public List<PushedLog> decodeProtobuf(byte[] body) {
        ExportLogsServiceRequest request;
        try {
            request = ExportLogsServiceRequest.parseFrom(body);
        } catch (InvalidProtocolBufferException ex) {
            throw new IngestRejectedException(HttpStatus.BAD_REQUEST, "INVALID_OTLP", "Body is not a valid OTLP logs export");
        }
        List<PushedLog> out = new ArrayList<>();
        for (ResourceLogs resourceLogs : request.getResourceLogsList()) {
            Map<String, String> resource = protoAttributes(resourceLogs.getResource().getAttributesList());
            for (ScopeLogs scopeLogs : resourceLogs.getScopeLogsList()) {
                for (LogRecord record : scopeLogs.getLogRecordsList()) {
                    Map<String, String> attributes = new LinkedHashMap<>(resource);
                    attributes.putAll(protoAttributes(record.getAttributesList()));
                    long nanos = record.getTimeUnixNano() != 0 ? record.getTimeUnixNano() : record.getObservedTimeUnixNano();
                    out.add(toPushedLog(nanos, record.getSeverityNumberValue(), record.getSeverityText(),
                            record.hasBody() ? render(record.getBody()) : null, attributes));
                }
            }
        }
        return out;
    }

    private static Map<String, String> protoAttributes(List<KeyValue> attributes) {
        Map<String, String> out = new LinkedHashMap<>();
        for (KeyValue kv : attributes) {
            String value = render(kv.getValue());
            if (value != null) {
                out.put(kv.getKey(), value);
            }
        }
        return out;
    }

    static String render(AnyValue value) {
        return switch (value.getValueCase()) {
            case STRING_VALUE -> value.getStringValue();
            case BOOL_VALUE -> Boolean.toString(value.getBoolValue());
            case INT_VALUE -> Long.toString(value.getIntValue());
            case DOUBLE_VALUE -> Double.toString(value.getDoubleValue());
            case ARRAY_VALUE -> value.getArrayValue().getValuesList().stream()
                    .map(OtlpLogDecoder::render).collect(Collectors.joining(", ", "[", "]"));
            case KVLIST_VALUE -> value.getKvlistValue().getValuesList().stream()
                    .map(kv -> kv.getKey() + "=" + render(kv.getValue())).collect(Collectors.joining(", ", "{", "}"));
            case BYTES_VALUE -> "<" + value.getBytesValue().size() + " bytes>";
            default -> null;
        };
    }

    /* ── OTLP JSON ────────────────────────────────────────────── */

    /**
     * OTLP JSON: lowerCamelCase fields, 64-bit integers as strings, enums as numbers
     * (snake_case names are accepted as well).
     */
    public List<PushedLog> decodeJson(byte[] body) {
        JsonNode root;
        try {
            root = objectMapper.readTree(body);
        } catch (JacksonException ex) {
            throw new IngestRejectedException(HttpStatus.BAD_REQUEST, "INVALID_OTLP", "Body is not valid JSON");
        }
        List<PushedLog> out = new ArrayList<>();
        for (JsonNode resourceLogs : array(root, "resourceLogs", "resource_logs")) {
            JsonNode resourceNode = resourceLogs.get("resource");
            Map<String, String> resource = jsonAttributes(resourceNode == null ? null : resourceNode.get("attributes"));
            List<JsonNode> scopes = new ArrayList<>(array(resourceLogs, "scopeLogs", "scope_logs"));
            scopes.addAll(array(resourceLogs, "instrumentationLibraryLogs")); // pre-1.0 exporters
            for (JsonNode scopeLogs : scopes) {
                for (JsonNode record : array(scopeLogs, "logRecords", "log_records")) {
                    Map<String, String> attributes = new LinkedHashMap<>(resource);
                    attributes.putAll(jsonAttributes(record.get("attributes")));
                    long nanos = longField(record, "timeUnixNano", "time_unix_nano");
                    if (nanos == 0) {
                        nanos = longField(record, "observedTimeUnixNano", "observed_time_unix_nano");
                    }
                    out.add(toPushedLog(nanos, (int) longField(record, "severityNumber", "severity_number"),
                            textField(record, "severityText", "severity_text"), renderJson(record.get("body")), attributes));
                }
            }
        }
        return out;
    }

    private static List<JsonNode> array(JsonNode node, String... names) {
        if (node == null) {
            return List.of();
        }
        for (String name : names) {
            JsonNode value = node.get(name);
            if (value != null && value.isArray()) {
                List<JsonNode> out = new ArrayList<>(value.size());
                value.forEach(out::add);
                return out;
            }
        }
        return List.of();
    }

    private static long longField(JsonNode node, String... names) {
        for (String name : names) {
            JsonNode value = node.get(name);
            if (value != null && !value.isNull()) {
                try {
                    return value.isNumber() ? value.asLong() : Long.parseLong(value.asString());
                } catch (NumberFormatException ex) {
                    return 0;
                }
            }
        }
        return 0;
    }

    private static String textField(JsonNode node, String... names) {
        for (String name : names) {
            JsonNode value = node.get(name);
            if (value != null && !value.isNull()) {
                return value.asString();
            }
        }
        return null;
    }

    private static Map<String, String> jsonAttributes(JsonNode attributes) {
        Map<String, String> out = new LinkedHashMap<>();
        if (attributes == null || !attributes.isArray()) {
            return out;
        }
        for (JsonNode kv : attributes) {
            JsonNode key = kv.get("key");
            String value = renderJson(kv.get("value"));
            if (key != null && value != null) {
                out.put(key.asString(), value);
            }
        }
        return out;
    }

    /** OTLP JSON AnyValue: {"stringValue": …} | {"intValue": "1"} | {"arrayValue": {"values": […]}} … */
    static String renderJson(JsonNode value) {
        if (value == null || value.isNull() || !value.isObject()) {
            return null;
        }
        for (String field : new String[] {"stringValue", "string_value"}) {
            if (value.has(field)) {
                return value.get(field).asString();
            }
        }
        for (String field : new String[] {"boolValue", "intValue", "doubleValue", "bool_value", "int_value", "double_value"}) {
            if (value.has(field)) {
                return value.get(field).asString();
            }
        }
        JsonNode array = value.has("arrayValue") ? value.get("arrayValue") : value.get("array_value");
        if (array != null) {
            List<String> items = new ArrayList<>();
            array(array, "values").forEach(v -> items.add(renderJson(v)));
            return "[" + String.join(", ", items) + "]";
        }
        JsonNode kvlist = value.has("kvlistValue") ? value.get("kvlistValue") : value.get("kvlist_value");
        if (kvlist != null) {
            List<String> items = new ArrayList<>();
            array(kvlist, "values").forEach(kv -> items.add(kv.get("key").asString() + "=" + renderJson(kv.get("value"))));
            return "{" + String.join(", ", items) + "}";
        }
        return null;
    }

    /* ── shared mapping ───────────────────────────────────────── */

    static PushedLog toPushedLog(long timeUnixNano, int severityNumber, String severityText, String body,
                                 Map<String, String> attributes) {
        Map<String, String> labels = new LinkedHashMap<>(attributes);
        String service = labels.remove(SERVICE);
        String namespace = labels.remove(NAMESPACE);
        String pod = labels.remove(POD);
        String container = labels.remove(CONTAINER);
        String containerAlt = labels.remove(CONTAINER_ALT);
        String cluster = labels.remove(CLUSTER);
        String stackTrace = labels.remove(STACKTRACE);
        String exceptionType = labels.get(EXCEPTION_TYPE);
        String exceptionMessage = labels.get(EXCEPTION_MESSAGE);

        String message = body;
        if (message == null || message.isBlank()) {
            // A record that only carries an exception (common for logger.error(msg, ex) bridges).
            message = exceptionType == null ? exceptionMessage
                    : exceptionMessage == null ? exceptionType : exceptionType + ": " + exceptionMessage;
        }

        Instant timestamp = timeUnixNano <= 0 ? null
                : Instant.ofEpochSecond(timeUnixNano / 1_000_000_000L, timeUnixNano % 1_000_000_000L);
        Severity severity = fromSeverityNumber(severityNumber);
        if (severity == null) {
            severity = IngestService.parseSeverity(severityText);
        }
        return new PushedLog(timestamp, severity, message, stackTrace, service, namespace, pod,
                container != null ? container : containerAlt, cluster, labels);
    }

    /** OTLP SeverityNumber: 1–4 TRACE, 5–8 DEBUG, 9–12 INFO, 13–16 WARN, 17–20 ERROR, 21–24 FATAL. */
    static Severity fromSeverityNumber(int number) {
        if (number <= 0) {
            return null;
        }
        if (number <= 4) {
            return Severity.TRACE;
        }
        if (number <= 8) {
            return Severity.DEBUG;
        }
        if (number <= 12) {
            return Severity.INFO;
        }
        if (number <= 16) {
            return Severity.WARN;
        }
        if (number <= 20) {
            return Severity.ERROR;
        }
        return Severity.CRITICAL;
    }
}
