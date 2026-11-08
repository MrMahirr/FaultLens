package com.faultlens.api.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import tools.jackson.databind.JsonNode;

/**
 * GET /deployments filters in the database (P10 / 1.3 follow-up A5): the page used to filter the
 * newest 50 rows in the browser, so older matches were never shown.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DeploymentSearchIT extends AbstractIntegrationTest {
    /** Unique per run: the database is shared with the other ITs. */
    private final String marker = "dsit" + UUID.randomUUID().toString().substring(0, 8);
    private String token;

    @BeforeAll
    void seed() {
        token = adminToken();
        Instant now = Instant.now();
        // The oldest one is far behind 60 newer rows of the same marker.
        record(marker + "-payments", "PRODUCTION", now.minusSeconds(10_000));
        for (int i = 0; i < 60; i++) {
            record(marker + "-orders", "STAGING", now.minusSeconds(60 - i));
        }
        record(marker + "-pay_ment", "PRODUCTION", now.minusSeconds(5_000));
        record(marker + "-payxment", "PRODUCTION", now.minusSeconds(5_001));
    }

    @Test
    void findsAMatchOlderThanTheFirstPage() {
        assertThat(services(page("q=" + marker + "-PAYMENTS"))).containsExactly(marker + "-payments");
    }

    @Test
    void filtersByEnvironment() {
        JsonNode data = page("q=" + marker + "&environment=PRODUCTION&size=200").at("/data");
        assertThat(data.at("/total_elements").asLong()).isEqualTo(3);
        assertThat(services(data)).allMatch(service -> !service.endsWith("-orders"));
    }

    @Test
    void likeWildcardsInTheQueryAreLiteral() {
        // "_" would match any character ("payxment") if it were not escaped.
        assertThat(services(page("q=" + marker + "-pay_ment"))).containsExactly(marker + "-pay_ment");
        assertThat(page("q=" + marker + "%25").at("/data/total_elements").asLong()).isZero();
    }

    @Test
    void pagesNewestFirstWithTheTotal() {
        JsonNode data = page("q=" + marker + "-orders&size=25&page=2").at("/data");
        assertThat(data.at("/total_elements").asLong()).isEqualTo(60);
        assertThat(data.at("/total_pages").asInt()).isEqualTo(3);
        assertThat(data.at("/content").size()).isEqualTo(10);
    }

    @Test
    void rejectsAnOversizedPage() {
        assertThat(get("/api/v1/deployments?size=5000", token).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private void record(String service, String environment, Instant deployedAt) {
        ResponseEntity<JsonNode> created = exchange(HttpMethod.POST, "/api/v1/deployments", token, Map.of(
                "service_name", service, "version", "1.0.0", "environment", environment,
                "deployed_at", deployedAt.toString(), "deployed_by", "it", "status", "SUCCESS"));
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private JsonNode page(String query) {
        ResponseEntity<JsonNode> response = get("/api/v1/deployments?" + query, token);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    private static List<String> services(JsonNode body) {
        JsonNode content = body.has("data") ? body.at("/data/content") : body.at("/content");
        List<String> names = new ArrayList<>();
        content.forEach(item -> names.add(item.at("/service_name").asText()));
        return names;
    }
}
