package com.faultlens.api.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import tools.jackson.databind.JsonNode;

/**
 * P10 / 1.1 acceptance: CI records deployments with an API key instead of a user JWT; keys are
 * confined to their scopes, stored only as a hash, and revocation is immediate.
 */
class ApiKeyIT extends AbstractIntegrationTest {

    private record Created(long id, String token, JsonNode body) {
    }

    private Created createKey(List<String> scopes, Integer expiresInDays) {
        Map<String, Object> request = new java.util.HashMap<>();
        request.put("name", "ci-" + UUID.randomUUID().toString().substring(0, 8));
        request.put("scopes", scopes);
        if (expiresInDays != null) {
            request.put("expires_in_days", expiresInDays);
        }
        ResponseEntity<JsonNode> created = exchange(HttpMethod.POST, "/api/v1/api-keys", adminToken(), request);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode data = created.getBody().at("/data");
        return new Created(data.at("/key/id").asLong(), data.at("/token").asText(), data);
    }

    private ResponseEntity<JsonNode> recordDeployment(String token) {
        return exchange(HttpMethod.POST, "/api/v1/deployments", token, Map.of(
                "service_name", "orders", "version", "9.9.9", "environment", "PRODUCTION",
                "deployed_at", Instant.now().toString(), "deployed_by", "ci"));
    }

    @Test
    void ciRecordsADeploymentWithAScopedKey() {
        Created key = createKey(List.of("deployments:write"), null);

        assertThat(key.token()).startsWith("flk_");
        assertThat(key.body().at("/key/display_key").asText()).endsWith("_…").doesNotContain(key.token().substring(13));
        assertThat(recordDeployment(key.token()).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void keyCannotLeaveItsScope() {
        Created key = createKey(List.of("deployments:write"), null);

        for (String[] call : new String[][] {
                {"GET", "/api/v1/deployments"}, {"GET", "/api/v1/logs"}, {"GET", "/api/v1/users"},
                {"GET", "/api/v1/api-keys"}, {"GET", "/api/v1/settings/system"}}) {
            ResponseEntity<JsonNode> denied = exchange(HttpMethod.valueOf(call[0]), call[1], key.token(), null);
            assertThat(denied.getStatusCode()).as("%s %s", call[0], call[1]).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(denied.getBody().at("/error/code").asText()).isEqualTo("API_KEY_SCOPE");
        }
    }

    @Test
    void readScopeReadsLogsButCannotRecordDeployments() {
        Created key = createKey(List.of("logs:read"), null);

        assertThat(get("/api/v1/logs", key.token()).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(recordDeployment(key.token()).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void revokedAndForgedKeysAreRejected() {
        Created key = createKey(List.of("deployments:write"), 30);
        assertThat(key.body().at("/key/expires_at").isNull()).isFalse();

        assertThat(exchange(HttpMethod.DELETE, "/api/v1/api-keys/" + key.id(), adminToken(), null)
                .getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        ResponseEntity<JsonNode> revoked = recordDeployment(key.token());
        assertThat(revoked.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(revoked.getBody().at("/error/code").asText()).isEqualTo("INVALID_API_KEY");

        String forged = key.token().substring(0, 13) + "A".repeat(32);
        assertThat(recordDeployment(forged).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        JsonNode listed = null;
        for (JsonNode k : get("/api/v1/api-keys", adminToken()).getBody().at("/data")) {
            if (k.at("/id").asLong() == key.id()) {
                listed = k;
            }
        }
        assertThat(listed).isNotNull();
        assertThat(listed.at("/active").asBoolean()).isFalse();
        assertThat(listed.toString()).doesNotContain(key.token().substring(13));
    }

    @Test
    void unknownScopesAreRejectedAndOnlyAdminsManageKeys() {
        ResponseEntity<JsonNode> bad = exchange(HttpMethod.POST, "/api/v1/api-keys", adminToken(),
                Map.of("name", "x", "scopes", List.of("admin")));
        assertThat(bad.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(bad.getBody().at("/error/code").asText()).isEqualTo("INVALID_SCOPE");

        assertThat(get("/api/v1/api-keys", null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
