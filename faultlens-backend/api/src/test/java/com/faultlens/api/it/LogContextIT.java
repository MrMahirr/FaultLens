package com.faultlens.api.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;

/**
 * P10 / 2.3: the context of an entry is its own source and pod, ±60 minutes, oldest first,
 * ordered by (timestamp, id); paging from the edge entries neither skips nor repeats; the
 * context index is valid and used.
 */
class LogContextIT extends AbstractIntegrationTest {
    @Autowired
    private JdbcTemplate jdbc;

    private final Instant base = Instant.now().minus(Duration.ofMinutes(30)).truncatedTo(ChronoUnit.SECONDS);

    @Test
    void contextIsTheSamePodAroundTheEntryInOrder() {
        String token = adminToken();
        long source = source();
        // Pod A: 120 entries one second apart, with three sharing one timestamp; pod B interleaved.
        List<Long> podA = new ArrayList<>();
        for (int i = 0; i < 120; i++) {
            int second = i >= 60 && i <= 62 ? 60 : i;
            podA.add(insert(source, "pod-a", base.plusSeconds(second), "a" + i));
            if (i % 2 == 0) {
                insert(source, "pod-b", base.plusSeconds(i), "b" + i);
            }
        }
        insert(source, "pod-a", base.plus(Duration.ofMinutes(90)), "far away"); // outside ±60 min

        JsonNode context = context(token, podA.get(61), 50, 50);
        List<Long> ids = ids(context);
        assertThat(ids).hasSize(101);
        assertThat(ids.get(50)).isEqualTo(podA.get(61));
        assertThat(ids).containsExactlyElementsOf(podA.subList(11, 112)); // same pod, (timestamp, id) order
        assertThat(context.at("/anchor_id").asLong()).isEqualTo(podA.get(61));
        assertThat(context.at("/has_more_before").asBoolean()).isTrue();
        assertThat(context.at("/has_more_after").asBoolean()).isTrue();
        assertThat(context.at("/window_minutes").asInt()).isEqualTo(60);
        assertThat(context.propertyNames()).containsExactlyInAnyOrderElementsOf(ApiContractIT.LOG_CONTEXT);

        // "Load more" above: the first entry's context, nothing after it — continues seamlessly.
        JsonNode above = context(token, podA.get(11), 50, 0);
        assertThat(ids(above)).containsExactlyElementsOf(podA.subList(0, 12));
        assertThat(above.at("/has_more_before").asBoolean()).isFalse();

        // Below the last entry: the rest of pod A within the window, not the entry 90 min away.
        JsonNode below = context(token, podA.get(111), 0, 50);
        assertThat(ids(below)).containsExactlyElementsOf(podA.subList(111, 120));
        assertThat(below.at("/has_more_after").asBoolean()).isFalse();
    }

    @Test
    void entriesWithoutAPodShareTheirSourcesContext() {
        String token = adminToken();
        long source = source();
        long first = insert(source, null, base, "no pod 1");
        long second = insert(source, null, base.plusSeconds(1), "no pod 2");
        insert(source, "pod-x", base.plusSeconds(1), "with pod");

        assertThat(ids(context(token, first, 5, 5))).containsExactly(first, second);
    }

    @Test
    void theContextIndexIsValidAndUsed() {
        assertThat(jdbc.queryForObject("""
                select x.indisvalid from pg_index x join pg_class c on c.oid = x.indexrelid
                 where c.relname = 'idx_log_entries_context'
                """, Boolean.class)).isTrue();
        long source = source();
        long id = insert(source, "pod-plan", base, "plan");
        String explain = """
                EXPLAIN SELECT * FROM log_entries
                 WHERE source_id = %d AND pod_name = 'pod-plan' AND (timestamp, id) < ('%s'::timestamptz, %d)
                   AND timestamp >= '%s'::timestamptz
                 ORDER BY timestamp DESC, id DESC LIMIT 51
                """.formatted(source, base, id, base.minus(Duration.ofHours(1)));
        // A test table is tiny, where a sequential scan is cheaper; the question is whether the
        // index can serve the query (scale: see the P10 2.3 benchmark in the vault).
        String plan = jdbc.execute((org.springframework.jdbc.core.ConnectionCallback<String>) connection -> {
            try (var statement = connection.createStatement()) {
                statement.execute("SET enable_seqscan = off");
                StringBuilder lines = new StringBuilder();
                try (var rs = statement.executeQuery(explain)) {
                    while (rs.next()) {
                        lines.append(rs.getString(1)).append('\n');
                    }
                } finally {
                    statement.execute("RESET enable_seqscan");
                }
                return lines.toString();
            }
        });
        assertThat(plan).contains("context").contains("Index");
    }

    @Test
    void unknownEntriesAndTooManyLinesAreRejected() {
        String token = adminToken();
        assertThat(get("/api/v1/logs/999999999999/context", token).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        long id = insert(source(), "p", base, "x");
        assertThat(get("/api/v1/logs/" + id + "/context?before=201", token).getStatusCode().is4xxClientError())
                .isTrue();
    }

    private JsonNode context(String token, long id, int before, int after) {
        ResponseEntity<JsonNode> response = get("/api/v1/logs/" + id + "/context?before=" + before + "&after=" + after,
                token);
        assertThat(response.getStatusCode()).as("%s", response.getBody()).isEqualTo(HttpStatus.OK);
        return response.getBody().at("/data");
    }

    private static List<Long> ids(JsonNode context) {
        List<Long> ids = new ArrayList<>();
        context.at("/entries").forEach(entry -> ids.add(entry.at("/id").asLong()));
        return ids;
    }

    private long source() {
        return jdbc.queryForObject("""
                INSERT INTO log_sources (name, type, config, enabled) VALUES (?, 'DOCKER', '{}'::jsonb, false)
                RETURNING id
                """, Long.class, "context-it-" + UUID.randomUUID());
    }

    private long insert(long source, String pod, Instant timestamp, String message) {
        return jdbc.queryForObject("""
                INSERT INTO log_entries (source_id, severity, message, pod_name, timestamp)
                VALUES (?, 'INFO', ?, ?, ?) RETURNING id
                """, Long.class, source, message, pod, Timestamp.from(timestamp));
    }
}
