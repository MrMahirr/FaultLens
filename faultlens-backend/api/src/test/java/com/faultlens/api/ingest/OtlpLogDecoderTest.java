package com.faultlens.api.ingest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.faultlens.common.enums.Severity;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.logs.v1.LogRecord;
import io.opentelemetry.proto.logs.v1.ResourceLogs;
import io.opentelemetry.proto.logs.v1.ScopeLogs;
import io.opentelemetry.proto.logs.v1.SeverityNumber;
import io.opentelemetry.proto.resource.v1.Resource;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class OtlpLogDecoderTest {
    private static final long NANOS = 1_762_509_600_123_456_789L; // 2025-11-07T10:00:00.123456789Z

    private final OtlpLogDecoder decoder = new OtlpLogDecoder(new ObjectMapper());

    private static KeyValue attr(String key, String value) {
        return KeyValue.newBuilder().setKey(key).setValue(AnyValue.newBuilder().setStringValue(value)).build();
    }

    @Test
    void mapsResourceAndRecordAttributesFromProtobuf() {
        ExportLogsServiceRequest request = ExportLogsServiceRequest.newBuilder()
                .addResourceLogs(ResourceLogs.newBuilder()
                        .setResource(Resource.newBuilder()
                                .addAttributes(attr("service.name", "orders"))
                                .addAttributes(attr("k8s.namespace.name", "prod"))
                                .addAttributes(attr("k8s.pod.name", "orders-7d9f"))
                                .addAttributes(attr("k8s.container.name", "app"))
                                .addAttributes(attr("k8s.cluster.name", "eu-1")))
                        .addScopeLogs(ScopeLogs.newBuilder()
                                .addLogRecords(LogRecord.newBuilder()
                                        .setTimeUnixNano(NANOS)
                                        .setSeverityNumber(SeverityNumber.SEVERITY_NUMBER_ERROR)
                                        .setBody(AnyValue.newBuilder().setStringValue("Payment failed"))
                                        .addAttributes(attr("exception.stacktrace", "java.lang.IllegalStateException: x\n\tat a.B.c(B.java:1)"))
                                        .addAttributes(attr("order.id", "42"))
                                        .addAttributes(KeyValue.newBuilder().setKey("retries")
                                                .setValue(AnyValue.newBuilder().setIntValue(3))))))
                .build();

        List<PushedLog> logs = decoder.decodeProtobuf(request.toByteArray());

        assertThat(logs).hasSize(1);
        PushedLog log = logs.getFirst();
        assertThat(log.timestamp()).isEqualTo(Instant.ofEpochSecond(1_762_509_600L, 123_456_789L));
        assertThat(log.severity()).isEqualTo(Severity.ERROR);
        assertThat(log.message()).isEqualTo("Payment failed");
        assertThat(log.stackTrace()).startsWith("java.lang.IllegalStateException");
        assertThat(log.serviceName()).isEqualTo("orders");
        assertThat(log.namespace()).isEqualTo("prod");
        assertThat(log.podName()).isEqualTo("orders-7d9f");
        assertThat(log.containerName()).isEqualTo("app");
        assertThat(log.cluster()).isEqualTo("eu-1");
        assertThat(log.attributes()).containsEntry("order.id", "42").containsEntry("retries", "3")
                .doesNotContainKeys("service.name", "exception.stacktrace");
    }

    @Test
    void decodesTheOtlpJsonEncoding() {
        String json = """
                {"resourceLogs":[{"resource":{"attributes":[{"key":"service.name","value":{"stringValue":"web"}}]},
                  "scopeLogs":[{"logRecords":[
                    {"timeUnixNano":"%d","severityNumber":13,"body":{"stringValue":"slow request"},
                     "attributes":[{"key":"http.status","value":{"intValue":"504"}}]},
                    {"observedTimeUnixNano":"%d","severityText":"fatal","body":{"stringValue":"out of memory"}}
                  ]}]}]}""".formatted(NANOS, NANOS);

        List<PushedLog> logs = decoder.decodeJson(json.getBytes(StandardCharsets.UTF_8));

        assertThat(logs).hasSize(2);
        assertThat(logs.get(0).severity()).isEqualTo(Severity.WARN);
        assertThat(logs.get(0).serviceName()).isEqualTo("web");
        assertThat(logs.get(0).attributes()).containsEntry("http.status", "504");
        assertThat(logs.get(1).severity()).isEqualTo(Severity.CRITICAL); // from severity_text
        assertThat(logs.get(1).timestamp()).isNotNull();                 // observed time as fallback
    }

    @Test
    void usesTheExceptionWhenTheRecordHasNoBody() {
        PushedLog log = OtlpLogDecoder.toPushedLog(0, 17, null, null, new java.util.LinkedHashMap<>(java.util.Map.of(
                "exception.type", "java.net.SocketTimeoutException", "exception.message", "Read timed out")));

        assertThat(log.message()).isEqualTo("java.net.SocketTimeoutException: Read timed out");
        assertThat(log.timestamp()).isNull();
    }

    @Test
    void mapsSeverityNumberRanges() {
        assertThat(OtlpLogDecoder.fromSeverityNumber(0)).isNull();
        assertThat(OtlpLogDecoder.fromSeverityNumber(1)).isEqualTo(Severity.TRACE);
        assertThat(OtlpLogDecoder.fromSeverityNumber(8)).isEqualTo(Severity.DEBUG);
        assertThat(OtlpLogDecoder.fromSeverityNumber(9)).isEqualTo(Severity.INFO);
        assertThat(OtlpLogDecoder.fromSeverityNumber(16)).isEqualTo(Severity.WARN);
        assertThat(OtlpLogDecoder.fromSeverityNumber(20)).isEqualTo(Severity.ERROR);
        assertThat(OtlpLogDecoder.fromSeverityNumber(24)).isEqualTo(Severity.CRITICAL);
    }

    @Test
    void rejectsGarbage() {
        assertThatThrownBy(() -> decoder.decodeProtobuf(new byte[] {(byte) 0xff, 0x01, 0x02}))
                .isInstanceOf(IngestRejectedException.class).hasMessageContaining("OTLP");
        assertThatThrownBy(() -> decoder.decodeJson("{not json".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(IngestRejectedException.class);
    }
}
