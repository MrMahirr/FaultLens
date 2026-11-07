package com.faultlens.api.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.faultlens.processor.model.LogEntry;
import com.faultlens.processor.repository.LogEntryRepository;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.logs.v1.LogRecord;
import io.opentelemetry.proto.logs.v1.ResourceLogs;
import io.opentelemetry.proto.logs.v1.ScopeLogs;
import io.opentelemetry.proto.logs.v1.SeverityNumber;
import io.opentelemetry.proto.resource.v1.Resource;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import tools.jackson.databind.JsonNode;

/**
 * P10 / 1.2 acceptance: applications push logs (simple JSON and OTLP/HTTP protobuf) with a
 * logs:ingest key bound to an HTTP source; they go through the normal pipeline (processor,
 * grouping) and show up under that source.
 */
class IngestIT extends AbstractIntegrationTest {
    private static final Duration TIMEOUT = Duration.ofSeconds(60);

    @Autowired
    private LogEntryRepository entries;

    private record Ingest(long sourceId, String token) {
    }

    private Ingest httpSourceWithKey() {
        ResponseEntity<JsonNode> source = exchange(HttpMethod.POST, "/api/v1/sources", adminToken(),
                Map.of("name", "http-it-" + UUID.randomUUID(), "type", "HTTP", "config", "{}", "enabled", true));
        assertThat(source.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        long sourceId = source.getBody().at("/data/id").asLong();

        ResponseEntity<JsonNode> key = exchange(HttpMethod.POST, "/api/v1/api-keys", adminToken(),
                Map.of("name", "ingest-it", "scopes", List.of("logs:ingest"), "source_id", sourceId));
        assertThat(key.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(key.getBody().at("/data/key/source_id").asLong()).isEqualTo(sourceId);
        return new Ingest(sourceId, key.getBody().at("/data/token").asText());
    }

    private Optional<LogEntry> stored(long sourceId, String marker) {
        return entries.findAll().stream()
                .filter(e -> e.getSourceId() != null && e.getSourceId() == sourceId)
                .filter(e -> e.getMessage() != null && e.getMessage().contains(marker))
                .findFirst();
    }

    @Test
    void jsonLogsReachThePipelineUnderTheKeysSource() {
        Ingest ingest = httpSourceWithKey();
        String marker = "IngestIT-" + UUID.randomUUID();

        ResponseEntity<JsonNode> accepted = exchange(HttpMethod.POST, "/api/v1/ingest/logs", ingest.token(), List.of(
                Map.of("message", "checkout failed " + marker, "severity", "ERROR", "service_name", "orders",
                        "timestamp", Instant.now().toString(), "attributes", Map.of("sourceId", "1")),
                Map.of("message", "info line " + marker)));

        assertThat(accepted.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(accepted.getBody().at("/data/accepted").asInt()).isEqualTo(2);
        LogEntry entry = await().atMost(TIMEOUT).until(() -> stored(ingest.sourceId(), "checkout failed " + marker), Optional::isPresent).orElseThrow();
        assertThat(entry.getServiceName()).isEqualTo("orders");
        assertThat(entry.getGroupId()).isNotNull();

        // The source shows as connected once something was pushed.
        await().atMost(TIMEOUT).until(() -> get("/api/v1/sources", adminToken()).getBody().at("/data").valueStream()
                .anyMatch(s -> s.at("/id").asLong() == ingest.sourceId() && "CONNECTED".equals(s.at("/health").asString())));
    }

    @Test
    void otlpProtobufLogsReachThePipeline() {
        Ingest ingest = httpSourceWithKey();
        String marker = "OtlpIT-" + UUID.randomUUID();
        long nanos = Instant.now().toEpochMilli() * 1_000_000L;
        ExportLogsServiceRequest export = ExportLogsServiceRequest.newBuilder()
                .addResourceLogs(ResourceLogs.newBuilder()
                        .setResource(Resource.newBuilder().addAttributes(KeyValue.newBuilder()
                                .setKey("service.name").setValue(AnyValue.newBuilder().setStringValue("payments"))))
                        .addScopeLogs(ScopeLogs.newBuilder().addLogRecords(LogRecord.newBuilder()
                                .setTimeUnixNano(nanos)
                                .setSeverityNumber(SeverityNumber.SEVERITY_NUMBER_WARN)
                                .setBody(AnyValue.newBuilder().setStringValue("card declined " + marker)))))
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(ingest.token());
        headers.setContentType(MediaType.parseMediaType("application/x-protobuf"));
        ResponseEntity<byte[]> response = rest.exchange("/v1/logs", HttpMethod.POST,
                new HttpEntity<>(export.toByteArray(), headers), byte[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType().toString()).startsWith("application/x-protobuf");
        LogEntry entry = await().atMost(TIMEOUT).until(() -> stored(ingest.sourceId(), marker), Optional::isPresent).orElseThrow();
        assertThat(entry.getServiceName()).isEqualTo("payments");
        assertThat(entry.getSeverity().name()).isEqualTo("WARN");
    }

    @Test
    void userTokensAndUnboundKeysCannotIngest() {
        assertThat(exchange(HttpMethod.POST, "/api/v1/ingest/logs", adminToken(), List.of(Map.of("message", "x")))
                .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<JsonNode> noSource = exchange(HttpMethod.POST, "/api/v1/api-keys", adminToken(),
                Map.of("name", "bad", "scopes", List.of("logs:ingest")));
        assertThat(noSource.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(noSource.getBody().at("/error/code").asText()).isEqualTo("INVALID_API_KEY_SOURCE");

        ResponseEntity<JsonNode> deployKey = exchange(HttpMethod.POST, "/api/v1/api-keys", adminToken(),
                Map.of("name", "deploy", "scopes", List.of("deployments:write")));
        String token = deployKey.getBody().at("/data/token").asText();
        ResponseEntity<JsonNode> denied = exchange(HttpMethod.POST, "/api/v1/ingest/logs", token, List.of(Map.of("message", "x")));
        assertThat(denied.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(denied.getBody().at("/error/code").asText()).isEqualTo("API_KEY_SCOPE");
    }

    @Test
    void disabledSourcesAndInvalidPayloadsAreRefused() {
        Ingest ingest = httpSourceWithKey();

        ResponseEntity<JsonNode> invalid = exchange(HttpMethod.POST, "/api/v1/ingest/logs", ingest.token(),
                List.of(Map.of("severity", "ERROR")));
        assertThat(invalid.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(invalid.getBody().at("/error/code").asText()).isEqualTo("INVALID_LOG_RECORD");

        assertThat(exchange(HttpMethod.POST, "/api/v1/sources/" + ingest.sourceId() + "/disable", adminToken(), null)
                .getStatusCode()).isEqualTo(HttpStatus.OK);
        ResponseEntity<JsonNode> disabled = exchange(HttpMethod.POST, "/api/v1/ingest/logs", ingest.token(),
                List.of(Map.of("message", "x")));
        assertThat(disabled.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(disabled.getBody().at("/error/code").asText()).isEqualTo("SOURCE_DISABLED");
    }
}
