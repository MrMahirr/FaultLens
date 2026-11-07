package com.faultlens.api.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.faultlens.collector.kafka.LogEventProducer;
import com.faultlens.common.dto.LogEventDto;
import com.faultlens.common.enums.Severity;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Pins the JSON shape (exact field names) of every read endpoint and of the create endpoints.
 *
 * <p>The frontend reads these fields by name, so a renamed or dropped field is a breaking change.
 * P05's Jackson 3 migration changes naming/serialization defaults; this test turns any drift
 * into a failing build instead of a blank UI column. If a change is intentional, update the
 * expected set here and the frontend types together.
 *
 * <p>The sets are package-visible: OpenApiContractIT checks the OpenAPI schemas against them, so
 * the published contract (and the TypeScript types generated from it) matches the real JSON.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApiContractIT extends AbstractIntegrationTest {
    static final Set<String> ENVELOPE = Set.of("success", "data", "message", "timestamp");
    static final Set<String> ERROR_ENVELOPE = Set.of("success", "error");
    static final Set<String> ERROR_BODY = Set.of("code", "message", "timestamp");
    static final Set<String> PAGE = Set.of("content", "page", "size", "total_elements", "total_pages");

    static final Set<String> ALARM = Set.of("id", "rule_id", "rule_name", "source_id", "severity", "status",
            "message", "triggered_at", "resolved_at", "alarm_rule_id");
    static final Set<String> ALARM_STATS = Set.of("active", "acknowledged", "resolved", "open_critical");
    static final Set<String> LOG_ENTRY = Set.of("id", "source_id", "group_id", "severity", "message",
            "parsed_message", "stack_trace", "namespace", "pod_name", "container_name", "service_name", "cluster",
            "timestamp");
    static final Set<String> LOG_GROUP = Set.of("id", "fingerprint", "first_message", "severity", "count",
            "first_seen_at", "last_seen_at");
    static final Set<String> LOG_STATS = Set.of("TRACE", "DEBUG", "INFO", "WARN", "ERROR", "CRITICAL", "total");
    static final Set<String> ANALYSIS = Set.of("id", "log_group_id", "log_entry_id", "source_id", "root_cause",
            "suggestion", "affected_deployment", "confidence_score", "engine_type", "analyzed_at");
    static final Set<String> DEPLOYMENT = Set.of("id", "service_name", "version", "environment", "deployed_at",
            "deployed_by", "status", "notes");
    static final Set<String> SOURCE = Set.of("id", "name", "type", "config", "enabled", "created_at", "status",
            "last_seen_at", "health", "last_error", "last_error_at", "collector_instance");
    static final Set<String> NOTIFICATION_SETTINGS = Set.of("email_enabled", "slack_enabled", "slack_webhook",
            "webhook_enabled", "webhook_url", "push_enabled", "mobile_enabled", "email", "email_available");
    static final Set<String> SYSTEM_SETTINGS = Set.of("log_retention_days", "default_engine", "language", "ai_available");
    static final Set<String> PROFILE = Set.of("username", "email", "role");
    static final Set<String> TOKEN = Set.of("token", "expires_at", "must_change_password");
    static final Set<String> PROFILE_UPDATE = Set.of("username", "email", "role", "token", "expires_at");

    @Autowired
    private LogEventProducer producer;

    private String token;

    /** Runs one error log through the pipeline so every list endpoint has at least one row. */
    @BeforeAll
    void seed() throws Exception {
        token = adminToken();
        String marker = "ApiContractIT" + UUID.randomUUID().toString().replace("-", "");
        String trace = "java.lang.IllegalStateException: contract\n\tat com.example." + marker + ".run(X.java:1)";
        producer.send(LogEventDto.builder()
                        .id(UUID.randomUUID().toString())
                        .source("it")
                        .serviceName("orders")
                        .severity(Severity.ERROR)
                        .message(trace)
                        .rawLine(trace)
                        .timestamp(Instant.now())
                        .build())
                .get(10, TimeUnit.SECONDS);
        await().atMost(Duration.ofSeconds(60)).until(() -> items(get("/api/v1/alarms", token)).stream()
                .anyMatch(alarm -> alarm.at("/message").asText().contains("Grup ID:")) && items(get("/api/v1/logs", token))
                .stream().anyMatch(entry -> entry.at("/stack_trace").asText().contains(marker)));
    }

    @Test
    void alarms() {
        assertPageOf(get("/api/v1/alarms", token), ALARM);
        assertObject(get("/api/v1/alarms/stats", token), ALARM_STATS);
    }

    @Test
    void logs() {
        assertPageOf(get("/api/v1/logs", token), LOG_ENTRY);
        assertPageOf(get("/api/v1/logs/groups", token), LOG_GROUP);
        assertObject(get("/api/v1/logs/stats", token), LOG_STATS);

        long groupId = items(get("/api/v1/logs/groups", token)).getFirst().at("/id").asLong();
        assertPageOf(get("/api/v1/logs/groups/" + groupId + "/entries", token), LOG_ENTRY);
    }

    @Test
    void analyses() {
        assertPageOf(get("/api/v1/analyses", token), ANALYSIS);

        long id = items(get("/api/v1/analyses", token)).getFirst().at("/id").asLong();
        assertObject(get("/api/v1/analyses/" + id, token), ANALYSIS);
    }

    @Test
    void deployments() {
        ResponseEntity<JsonNode> created = exchange(HttpMethod.POST, "/api/v1/deployments", token, Map.of(
                "service_name", "orders", "version", "1.2.3", "environment", "prod",
                "deployed_at", Instant.now().toString(), "deployed_by", "ci"));
        assertObject(created, DEPLOYMENT);
        assertThat(created.getBody().at("/data/service_name").asText()).isEqualTo("orders");

        assertPageOf(get("/api/v1/deployments", token), DEPLOYMENT);
    }

    @Test
    void sources() throws Exception {
        String config = new ObjectMapper().writeValueAsString(Map.of("logFilePath",
                Path.of(System.getProperty("java.io.tmpdir"), "faultlens-contract-it.log").toString()));
        ResponseEntity<JsonNode> created = exchange(HttpMethod.POST, "/api/v1/sources", token,
                Map.of("name", "contract-it", "type", "LOCAL_FILE", "config", config, "enabled", false));
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertKeys(created.getBody(), ENVELOPE);
        assertKeys(created.getBody().at("/data"), SOURCE);

        // Not paged: a plain array.
        ResponseEntity<JsonNode> list = get("/api/v1/sources", token);
        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertKeys(list.getBody(), ENVELOPE);
        assertThat(list.getBody().at("/data").isArray()).isTrue();
        list.getBody().at("/data").forEach(source -> assertKeys(source, SOURCE));
    }

    @Test
    void settingsAndProfile() {
        assertObject(get("/api/v1/settings/notifications", token), NOTIFICATION_SETTINGS);
        assertObject(get("/api/v1/settings/system", token), SYSTEM_SETTINGS);
        assertObject(get("/api/v1/auth/profile", token), PROFILE);
        assertObject(login("admin", ADMIN_PASSWORD), TOKEN);
        // Same username (other ITs share the admin token); a rename is covered by AuthServiceTest.
        String email = get("/api/v1/auth/profile", token).getBody().at("/data/email").asText();
        assertObject(exchange(HttpMethod.PUT, "/api/v1/auth/profile", token,
                Map.of("username", "admin", "email", email)), PROFILE_UPDATE);
    }

    @Test
    void errors() {
        ResponseEntity<JsonNode> notFound = exchange(HttpMethod.PUT, "/api/v1/alarms/999999999/ack", token, null);
        assertThat(notFound.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertKeys(notFound.getBody(), ERROR_ENVELOPE);
        assertKeys(notFound.getBody().at("/error"), ERROR_BODY);

        ResponseEntity<JsonNode> badParam = get("/api/v1/alarms?page=abc", token);
        assertThat(badParam.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertKeys(badParam.getBody(), ERROR_ENVELOPE);
    }

    private void assertObject(ResponseEntity<JsonNode> response, Set<String> expected) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertKeys(response.getBody(), ENVELOPE);
        assertKeys(response.getBody().at("/data"), expected);
    }

    private void assertPageOf(ResponseEntity<JsonNode> response, Set<String> itemFields) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertKeys(response.getBody(), ENVELOPE);
        assertKeys(response.getBody().at("/data"), PAGE);
        List<JsonNode> items = items(response);
        assertThat(items).as("page content").isNotEmpty();
        items.forEach(item -> assertKeys(item, itemFields));
    }

    private static List<JsonNode> items(ResponseEntity<JsonNode> response) {
        List<JsonNode> items = new ArrayList<>();
        response.getBody().at("/data/content").forEach(items::add);
        return items;
    }

    private static void assertKeys(JsonNode node, Set<String> expected) {
        assertThat(node.propertyNames())
                .as("fields of %s", node)
                .containsExactlyInAnyOrderElementsOf(expected);
    }
}
