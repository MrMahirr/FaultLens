package com.faultlens.api.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * P07 acceptance: roles are enforced by the API, not just hidden in the UI.
 */
class RolesIT extends AbstractIntegrationTest {
    private static final String INITIAL = "initial-pass-1";
    private static final String FINAL = "changed-pass-2";

    /** Creates a user through the admin API and completes its first-login password change. */
    private String tokenFor(String role) {
        String username = role.toLowerCase() + "-" + UUID.randomUUID().toString().substring(0, 8);
        ResponseEntity<JsonNode> created = exchange(HttpMethod.POST, "/api/v1/users", adminToken(),
                Map.of("username", username, "role", role, "password", INITIAL));
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody().at("/data/must_change_password").asBoolean()).isTrue();

        String restricted = login(username, INITIAL).getBody().at("/data/token").asText();
        ResponseEntity<JsonNode> changed = exchange(HttpMethod.PUT, "/api/v1/auth/password", restricted,
                Map.of("current_password", INITIAL, "new_password", FINAL));
        assertThat(changed.getStatusCode()).isEqualTo(HttpStatus.OK);
        return changed.getBody().at("/data/token").asText();
    }

    private long createSource(String token) throws Exception {
        String config = new ObjectMapper().writeValueAsString(Map.of("logFilePath",
                Path.of(System.getProperty("java.io.tmpdir"), "faultlens-roles-it.log").toString()));
        ResponseEntity<JsonNode> created = exchange(HttpMethod.POST, "/api/v1/sources", token,
                Map.of("name", "roles-it-" + UUID.randomUUID(), "type", "LOCAL_FILE", "config", config,
                        "enabled", false));
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return created.getBody().at("/data/id").asLong();
    }

    @Test
    void viewerReadsButCannotChangeOrDeleteSources() throws Exception {
        long sourceId = createSource(adminToken());
        String viewer = tokenFor("VIEWER");

        assertThat(get("/api/v1/sources", viewer).getStatusCode()).isEqualTo(HttpStatus.OK);
        ResponseEntity<JsonNode> denied = exchange(HttpMethod.DELETE, "/api/v1/sources/" + sourceId, viewer, null);
        assertThat(denied.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(denied.getBody().at("/error/code").asText()).isEqualTo("ACCESS_DENIED");
        assertThat(exchange(HttpMethod.POST, "/api/v1/sources/" + sourceId + "/enable", viewer, null)
                .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(get("/api/v1/users", viewer).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        assertThat(exchange(HttpMethod.DELETE, "/api/v1/sources/" + sourceId, adminToken(), null)
                .getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void editorEditsButOnlyAdminDeletes() throws Exception {
        String editor = tokenFor("EDITOR");
        long sourceId = createSource(editor);

        assertThat(exchange(HttpMethod.DELETE, "/api/v1/sources/" + sourceId, editor, null)
                .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(exchange(HttpMethod.DELETE, "/api/v1/sources/" + sourceId, adminToken(), null)
                .getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void adminManagesUsersButCannotRemoveTheLastAdmin() {
        ResponseEntity<JsonNode> users = get("/api/v1/users", adminToken());
        assertThat(users.getStatusCode()).isEqualTo(HttpStatus.OK);
        long adminId = 0;
        for (JsonNode user : users.getBody().at("/data")) {
            if (user.at("/username").asText().equals("admin")) {
                adminId = user.at("/id").asLong();
            }
        }
        ResponseEntity<JsonNode> self = exchange(HttpMethod.DELETE, "/api/v1/users/" + adminId, adminToken(), null);
        assertThat(self.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(self.getBody().at("/error/code").asText()).isEqualTo("CANNOT_DELETE_SELF");

        ResponseEntity<JsonNode> badRole = exchange(HttpMethod.POST, "/api/v1/users", adminToken(),
                Map.of("username", "x-" + UUID.randomUUID(), "role", "OWNER", "password", INITIAL));
        assertThat(badRole.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
    }
}
