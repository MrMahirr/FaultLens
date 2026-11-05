package com.faultlens.api.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.faultlens.collector.kafka.LogEventProducer;
import com.faultlens.common.dto.LogEventDto;
import com.faultlens.common.enums.Severity;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import tools.jackson.databind.JsonNode;

/**
 * Alarm rules end to end (P07 acceptance): rules persist, a threshold rule fires exactly once
 * per cooldown on real log traffic, a pattern rule fires on a matching line.
 */
class AlarmRuleIT extends AbstractIntegrationTest {
    private static final Set<String> RULE = Set.of("id", "name", "type", "severity", "window_sec", "threshold",
            "pattern", "source_ids", "channels", "cooldown_sec", "enabled", "created_by", "created_at", "updated_at");

    @Autowired
    private LogEventProducer producer;

    @Test
    void ruleIsStoredAndListed() {
        String token = adminToken();
        long id = createRule(token, Map.of("name", "kalıcı kural", "type", "NEW_GROUP", "severity", "CRITICAL"));
        try {
            ResponseEntity<JsonNode> list = get("/api/v1/alarms/rules", token);
            assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
            JsonNode stored = find(list.getBody().at("/data"), id);
            assertThat(stored).isNotNull();
            assertThat(stored.propertyNames()).containsExactlyInAnyOrderElementsOf(RULE);
            assertThat(stored.at("/created_by").asText()).isEqualTo("admin");
            assertThat(stored.at("/channels").get(0).asText()).isEqualTo("IN_APP");
        } finally {
            deleteRule(token, id);
        }
        assertThat(get("/api/v1/alarms/rules/" + id, token).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void invalidRulesAreRejected() {
        String token = adminToken();
        ResponseEntity<JsonNode> missingThreshold = exchange(HttpMethod.POST, "/api/v1/alarms/rules", token,
                Map.of("name", "x", "type", "THRESHOLD", "severity", "ERROR", "window_sec", 60));
        assertThat(missingThreshold.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(missingThreshold.getBody().at("/error/code").asText()).isEqualTo("INVALID_ALARM_RULE");

        ResponseEntity<JsonNode> badRegex = exchange(HttpMethod.POST, "/api/v1/alarms/rules", token,
                Map.of("name", "x", "type", "PATTERN", "severity", "ERROR", "pattern", "(unclosed"));
        assertThat(badRegex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void thresholdRuleFiresExactlyOnceWithinItsCooldown() throws Exception {
        String token = adminToken();
        // Applies to all sources; other ITs' errors may count too, which only makes it fire
        // earlier — the assertion is about how many alarms this rule raised.
        long id = createRule(token, Map.of("name", "ERROR eşiği", "type", "THRESHOLD", "severity", "ERROR",
                "window_sec", 60, "threshold", 3, "cooldown_sec", 900));
        try {
            String marker = "AlarmRuleIT" + UUID.randomUUID().toString().replace("-", "");
            for (int i = 0; i < 8; i++) {
                send("java.lang.IllegalStateException: threshold " + i + "\n\tat com.example." + marker + ".t" + i + "(X.java:1)");
            }

            await().atMost(Duration.ofSeconds(60)).until(() -> alarmsOfRule(token, id).size() >= 1);
            Thread.sleep(3000); // the remaining errors are processed meanwhile
            List<JsonNode> alarms = alarmsOfRule(token, id);
            assertThat(alarms).hasSize(1);
            assertThat(alarms.getFirst().at("/rule_name").asText()).isEqualTo("ERROR eşiği");
            assertThat(alarms.getFirst().at("/rule_id").asText()).isEqualTo("RULE:" + id);
            assertThat(alarms.getFirst().at("/severity").asText()).isEqualTo("ERROR");
        } finally {
            deleteRule(token, id);
        }
    }

    @Test
    void patternRuleFiresOnAMatchingLine() throws Exception {
        String token = adminToken();
        String marker = "PatternIT" + UUID.randomUUID().toString().replace("-", "");
        long id = createRule(token, Map.of("name", "bağlantı reddi", "type", "PATTERN", "severity", "WARN",
                "pattern", "Connection refused.*" + marker));
        try {
            send("java.net.ConnectException: Connection refused " + marker + "\n\tat com.example.Db.connect(Db.java:9)");

            await().atMost(Duration.ofSeconds(60)).until(() -> alarmsOfRule(token, id).size() == 1);
            assertThat(alarmsOfRule(token, id).getFirst().at("/message").asText()).contains(marker);
        } finally {
            deleteRule(token, id);
        }
    }

    private void send(String trace) throws Exception {
        producer.send(LogEventDto.builder()
                        .id(UUID.randomUUID().toString())
                        .source("it")
                        .serviceName("rules")
                        .severity(Severity.ERROR)
                        .message(trace)
                        .rawLine(trace)
                        .timestamp(Instant.now())
                        .build())
                .get(10, TimeUnit.SECONDS);
    }

    private long createRule(String token, Map<String, Object> body) {
        ResponseEntity<JsonNode> created = exchange(HttpMethod.POST, "/api/v1/alarms/rules", token, new HashMap<>(body));
        assertThat(created.getStatusCode()).as("%s", created.getBody()).isEqualTo(HttpStatus.CREATED);
        return created.getBody().at("/data/id").asLong();
    }

    private void deleteRule(String token, long id) {
        exchange(HttpMethod.DELETE, "/api/v1/alarms/rules/" + id, token, null);
    }

    private List<JsonNode> alarmsOfRule(String token, long ruleId) {
        List<JsonNode> result = new ArrayList<>();
        get("/api/v1/alarms?size=100", token).getBody().at("/data/content").forEach(alarm -> {
            if (alarm.at("/alarm_rule_id").asLong(-1) == ruleId) {
                result.add(alarm);
            }
        });
        return result;
    }

    private static JsonNode find(JsonNode array, long id) {
        for (JsonNode node : array) {
            if (node.at("/id").asLong() == id) {
                return node;
            }
        }
        return null;
    }
}
