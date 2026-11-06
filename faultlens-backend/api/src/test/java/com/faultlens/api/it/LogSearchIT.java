package com.faultlens.api.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;

/** P08: bounded log queries and trigram-backed substring search. */
class LogSearchIT extends AbstractIntegrationTest {
    @Autowired
    private JdbcTemplate jdbc;

    private void insert(String message, Instant at) {
        jdbc.update("insert into log_entries (severity, message, raw_line, timestamp) values ('ERROR', ?, ?, ?)",
                message, at + " ERROR " + message, java.sql.Timestamp.from(at));
    }

    private long total(String query) {
        ResponseEntity<JsonNode> response = get("/api/v1/logs?" + query, adminToken());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody().at("/data/total_elements").asLong();
    }

    @Test
    void withoutATimeRangeOnlyTheLastDayIsSearched() {
        String marker = "srch" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        insert("Payment " + marker + " declined", Instant.now().minus(1, ChronoUnit.HOURS));
        insert("Payment " + marker + " declined", Instant.now().minus(3, ChronoUnit.DAYS));

        assertThat(total("search=" + marker.toUpperCase())).isEqualTo(1); // case-insensitive, last 24 h
        assertThat(total("search=" + marker + "&from=1970-01-01T00:00:00Z")).isEqualTo(2); // explicit range
        assertThat(total("search=" + marker + "&to=" + Instant.now().minus(2, ChronoUnit.DAYS))).isEqualTo(1);
    }

    @Test
    void substringSearchUsesTheTrigramIndexes() {
        jdbc.execute("ANALYZE log_entries");
        jdbc.execute("SET enable_seqscan = off"); // tiny test table: make the index choice visible
        try {
            List<String> plan = jdbc.queryForList("EXPLAIN select id from log_entries where lower(message) like '%zqxw-term%' "
                    + "or lower(raw_line) like '%zqxw-term%'", String.class);
            assertThat(String.join("\n", plan))
                    // Partitions carry generated index names (log_entries_YYYYMMDD_lower_idx...).
                    .contains("Bitmap Index Scan")
                    .contains("Index Cond: (lower(message) ~~ '%zqxw-term%'::text)")
                    .contains("Index Cond: (lower(raw_line) ~~ '%zqxw-term%'::text)")
                    .doesNotContain("Seq Scan");
        } finally {
            jdbc.execute("RESET enable_seqscan");
        }
    }
}
