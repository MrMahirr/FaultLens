package com.faultlens.api.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.faultlens.common.dto.LogEventDto;
import com.faultlens.common.enums.Severity;
import com.faultlens.processor.service.LogProcessorService;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;

/**
 * P10 / 2.1 acceptance: a resolved group seen again is reopened as a regression and raises a
 * REGRESSION alarm; ignored groups are muted and leave the default list; an expired ignore
 * reopens without a regression; late entries older than the resolution do not regress.
 */
class GroupLifecycleIT extends AbstractIntegrationTest {
    @Autowired
    private LogProcessorService processor;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void resolvedGroupSeenAgainRegressesAndAlarms() {
        String token = adminToken();
        String marker = marker();
        long ruleId = createRule(token, Map.of("name", "regresyon " + marker, "type", "REGRESSION", "severity", "ERROR"));
        try {
            long groupId = process(marker, Instant.now());
            JsonNode group = group(token, groupId);
            assertThat(group.at("/status").asText()).isEqualTo("UNRESOLVED");
            assertThat(group.at("/regression_count").asInt()).isZero();

            JsonNode resolved = patch(token, groupId, Map.of("status", "RESOLVED"));
            assertThat(resolved.at("/data/status").asText()).isEqualTo("RESOLVED");
            assertThat(resolved.at("/data/resolved_by").asText()).isEqualTo("admin");

            ResponseEntity<JsonNode> again = exchange(HttpMethod.PATCH, "/api/v1/logs/groups/" + groupId, token,
                    Map.of("status", "RESOLVED"));
            assertThat(again.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(again.getBody().at("/error/code").asText()).isEqualTo("GROUP_STATUS_UNCHANGED");

            process(marker, Instant.now().plusSeconds(1));

            group = group(token, groupId);
            assertThat(group.at("/status").asText()).isEqualTo("UNRESOLVED");
            assertThat(group.at("/regression_count").asInt()).isEqualTo(1);
            assertThat(group.at("/regressed_at").isNull()).isFalse();
            assertThat(group.at("/resolved_at").isNull()).isTrue();
            assertThat(timelineTypes(token, groupId)).containsExactly("REGRESSED", "RESOLVED", "CREATED");
            JsonNode timeline = get("/api/v1/logs/groups/" + groupId + "/events", token).getBody().at("/data");
            assertThat(timeline.get(0).propertyNames()).containsExactlyInAnyOrderElementsOf(ApiContractIT.LOG_GROUP_EVENT);
            assertThat(timeline.get(0).at("/regression_count").asInt()).isEqualTo(1);
            assertThat(timeline.get(0).at("/actor").isNull()).isTrue();
            assertThat(timeline.get(1).at("/actor").asText()).isEqualTo("admin");
            assertThat(timeline.get(1).at("/from_status").asText()).isEqualTo("UNRESOLVED");

            await().atMost(Duration.ofSeconds(5)).until(() -> alarmsOfRule(token, ruleId).size() == 1);
            assertThat(alarmsOfRule(token, ruleId).getFirst().at("/message").asText()).contains("Grup ID: " + groupId);
        } finally {
            exchange(HttpMethod.DELETE, "/api/v1/alarms/rules/" + ruleId, token, null);
        }
    }

    @Test
    void lateEntriesOlderThanTheResolutionDoNotRegress() {
        String token = adminToken();
        String marker = marker();
        long groupId = process(marker, Instant.now());
        patch(token, groupId, Map.of("status", "RESOLVED"));

        process(marker, Instant.now().minus(Duration.ofMinutes(10)));

        JsonNode group = group(token, groupId);
        assertThat(group.at("/status").asText()).isEqualTo("RESOLVED");
        assertThat(group.at("/count").asLong()).isEqualTo(2);
        assertThat(group.at("/regression_count").asInt()).isZero();
    }

    @Test
    void ignoredGroupsAreMutedHiddenByDefaultAndReopenWhenTheIgnoreRunsOut() {
        String token = adminToken();
        String marker = marker();
        long ruleId = createRule(token, Map.of("name", "desen " + marker, "type", "PATTERN", "severity", "ERROR",
                "pattern", marker));
        try {
            long groupId = process(marker, Instant.now());
            await().atMost(Duration.ofSeconds(5)).until(() -> alarmsOfRule(token, ruleId).size() == 1);

            String until = Instant.now().plus(Duration.ofHours(1)).toString();
            JsonNode ignored = patch(token, groupId, Map.of("status", "IGNORED", "ignore_until", until));
            assertThat(ignored.at("/data/status").asText()).isEqualTo("IGNORED");
            assertThat(listedIds(token, "")).doesNotContain(groupId);
            assertThat(listedIds(token, "&status=IGNORED")).contains(groupId);

            // Muted: stored and counted, but the pattern rule (cooldown per group) would not
            // fire anyway, so check the group directly: still IGNORED, no regression.
            process(marker, Instant.now());
            assertThat(group(token, groupId).at("/status").asText()).isEqualTo("IGNORED");
            assertThat(group(token, groupId).at("/count").asLong()).isEqualTo(2);

            jdbc.update("update log_groups set ignore_until = now() - interval '1 minute' where id = ?", groupId);
            process(marker, Instant.now());

            JsonNode group = group(token, groupId);
            assertThat(group.at("/status").asText()).isEqualTo("UNRESOLVED");
            assertThat(group.at("/regression_count").asInt()).isZero();
            assertThat(group.at("/ignore_until").isNull()).isTrue();
            assertThat(timelineTypes(token, groupId)).containsExactly("UNIGNORED", "IGNORED", "CREATED");
        } finally {
            exchange(HttpMethod.DELETE, "/api/v1/alarms/rules/" + ruleId, token, null);
        }
    }

    @Test
    void entriesOfAnIgnoredGroupRaiseNoAlarms() {
        String token = adminToken();
        String marker = marker();
        long groupId = process(marker, Instant.now());
        patch(token, groupId, Map.of("status", "IGNORED"));
        long ruleId = createRule(token, Map.of("name", "desen " + marker, "type", "PATTERN", "severity", "ERROR",
                "pattern", marker));
        try {
            process(marker, Instant.now());
            process(marker, Instant.now());
            assertThat(alarmsOfRule(token, ruleId)).isEmpty();
        } finally {
            exchange(HttpMethod.DELETE, "/api/v1/alarms/rules/" + ruleId, token, null);
        }
    }

    @Test
    void invalidChangesAreRejected() {
        String token = adminToken();
        long groupId = process(marker(), Instant.now());

        ResponseEntity<JsonNode> pastIgnore = exchange(HttpMethod.PATCH, "/api/v1/logs/groups/" + groupId, token,
                Map.of("status", "IGNORED", "ignore_until", "2020-01-01T00:00:00Z"));
        assertThat(pastIgnore.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(pastIgnore.getBody().at("/error/code").asText()).isEqualTo("INVALID_GROUP_STATUS");

        assertThat(exchange(HttpMethod.PATCH, "/api/v1/logs/groups/999999999", token, Map.of("status", "RESOLVED"))
                .getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    /** Letters only: digits are normalized away by the fingerprint, letters keep groups apart. */
    private static String marker() {
        StringBuilder marker = new StringBuilder("GroupLifecycleIT");
        java.util.concurrent.ThreadLocalRandom random = java.util.concurrent.ThreadLocalRandom.current();
        for (int i = 0; i < 24; i++) {
            marker.append((char) ('a' + random.nextInt(26)));
        }
        return marker.toString();
    }

    /** Processes one ERROR entry of the marker's group and returns the group id. */
    private long process(String marker, Instant timestamp) {
        String message = "java.lang.IllegalStateException: " + marker;
        processor.processBatch(List.of(LogEventDto.builder()
                .id(UUID.randomUUID().toString())
                .source("it")
                .serviceName("lifecycle")
                .severity(Severity.ERROR)
                .message(message)
                .rawLine(message)
                .timestamp(timestamp)
                .build()));
        return jdbc.queryForObject("select id from log_groups where first_message like ?", Long.class, "%" + marker + "%");
    }

    private JsonNode group(String token, long id) {
        ResponseEntity<JsonNode> response = get("/api/v1/logs/groups/" + id, token);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody().at("/data");
    }

    private JsonNode patch(String token, long id, Map<String, Object> body) {
        ResponseEntity<JsonNode> response = exchange(HttpMethod.PATCH, "/api/v1/logs/groups/" + id, token, body);
        assertThat(response.getStatusCode()).as("%s", response.getBody()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    private List<String> timelineTypes(String token, long id) {
        List<String> types = new ArrayList<>();
        get("/api/v1/logs/groups/" + id + "/events", token).getBody().at("/data")
                .forEach(event -> types.add(event.at("/type").asText()));
        return types;
    }

    private List<Long> listedIds(String token, String query) {
        List<Long> ids = new ArrayList<>();
        get("/api/v1/logs/groups?size=1000" + query, token).getBody().at("/data/content")
                .forEach(group -> ids.add(group.at("/id").asLong()));
        return ids;
    }

    private long createRule(String token, Map<String, Object> body) {
        ResponseEntity<JsonNode> created = exchange(HttpMethod.POST, "/api/v1/alarms/rules", token, new HashMap<>(body));
        assertThat(created.getStatusCode()).as("%s", created.getBody()).isEqualTo(HttpStatus.CREATED);
        return created.getBody().at("/data/id").asLong();
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
}
