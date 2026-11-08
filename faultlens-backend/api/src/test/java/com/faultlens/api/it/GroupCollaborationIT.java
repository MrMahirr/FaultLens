package com.faultlens.api.it;

import static org.assertj.core.api.Assertions.assertThat;

import com.faultlens.common.dto.LogEventDto;
import com.faultlens.common.enums.Severity;
import com.faultlens.processor.service.LogProcessorService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;

/**
 * P10 / 2.2 acceptance: assigning, commenting on and merging log groups, with their permissions;
 * merged groups route their new entries to the target, and unmerging restores that.
 */
class GroupCollaborationIT extends AbstractIntegrationTest {
    private static final String INITIAL = "initial-pass-1";
    private static final String FINAL = "changed-pass-2";

    @Autowired
    private LogProcessorService processor;

    @Autowired
    private JdbcTemplate jdbc;

    private record User(String username, String token) {
    }

    @Test
    void assignAndFilterByAssignee() {
        String admin = adminToken();
        User editor = user("EDITOR");
        long groupId = process(marker());

        ResponseEntity<JsonNode> unknown = put(admin, groupId, Map.of("assignee", "nobody-" + marker()));
        assertThat(unknown.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(unknown.getBody().at("/error/code").asText()).isEqualTo("UNKNOWN_USER");

        ResponseEntity<JsonNode> assigned = put(admin, groupId, Map.of("assignee", editor.username()));
        assertThat(assigned.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(assigned.getBody().at("/data/assignee").asText()).isEqualTo(editor.username());
        assertThat(put(admin, groupId, Map.of("assignee", editor.username())).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        assertThat(listedIds(admin, "&assignee=" + editor.username())).containsExactly(groupId);
        assertThat(timelineTypes(admin, groupId)).first().isEqualTo("ASSIGNED");
        assertThat(get("/api/v1/logs/groups/assignees", editor.token()).getBody().at("/data").toString())
                .contains("\"username\":\"" + editor.username() + "\"");

        Map<String, Object> unassign = new HashMap<>();
        unassign.put("assignee", null);
        assertThat(put(editor.token(), groupId, unassign).getBody().at("/data/assignee").isNull()).isTrue();
        assertThat(timelineTypes(admin, groupId)).first().isEqualTo("UNASSIGNED");

        User viewer = user("VIEWER");
        assertThat(put(viewer.token(), groupId, Map.of("assignee", viewer.username())).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void commentsAreEditedByTheirAuthorAndDeletedByTheAuthorOrAnAdmin() {
        String admin = adminToken();
        User author = user("EDITOR");
        User other = user("EDITOR");
        long groupId = process(marker());
        String path = "/api/v1/logs/groups/" + groupId + "/comments";

        ResponseEntity<JsonNode> created = exchange(HttpMethod.POST, path, author.token(),
                Map.of("body", "  <b>not html</b> — pool size?  "));
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        long commentId = created.getBody().at("/data/id").asLong();
        assertThat(created.getBody().at("/data").propertyNames())
                .containsExactlyInAnyOrderElementsOf(ApiContractIT.LOG_GROUP_COMMENT);
        assertThat(created.getBody().at("/data/body").asText()).isEqualTo("<b>not html</b> — pool size?");
        assertThat(created.getBody().at("/data/author").asText()).isEqualTo(author.username());

        assertThat(exchange(HttpMethod.POST, path, author.token(), Map.of("body", " ")).getStatusCode())
                .isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(exchange(HttpMethod.PUT, path + "/" + commentId, other.token(), Map.of("body", "hijack"))
                .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        ResponseEntity<JsonNode> edited = exchange(HttpMethod.PUT, path + "/" + commentId, author.token(),
                Map.of("body", "pool size was 10"));
        assertThat(edited.getBody().at("/data/body").asText()).isEqualTo("pool size was 10");
        assertThat(edited.getBody().at("/data/edited_at").isNull()).isFalse();

        assertThat(exchange(HttpMethod.DELETE, path + "/" + commentId, other.token(), null).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(exchange(HttpMethod.DELETE, path + "/" + commentId, admin, null).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(get(path, author.token()).getBody().at("/data")).isEmpty();
        assertThat(timelineTypes(admin, groupId)).contains("COMMENTED");
    }

    @Test
    void mergedGroupsRouteNewEntriesToTheTargetUntilUnmerged() {
        String admin = adminToken();
        User editor = user("EDITOR");
        String sourceMarker = marker();
        String targetMarker = marker();
        long source = process(sourceMarker);
        long target = process(targetMarker);

        ResponseEntity<JsonNode> merged = merge(editor.token(), source, target);
        assertThat(merged.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(merged.getBody().at("/data/merged_into_id").asLong()).isEqualTo(target);
        assertThat(merge(editor.token(), source, target).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        long entryGroup = processEntryGroup(sourceMarker);
        assertThat(entryGroup).isEqualTo(target);
        assertThat(count(target)).isEqualTo(2);
        assertThat(count(source)).isEqualTo(1);
        assertThat(listedIds(admin, "&status=UNRESOLVED,RESOLVED,IGNORED")).contains(target).doesNotContain(source);
        assertThat(timelineTypes(admin, source)).first().isEqualTo("MERGED");
        JsonNode mergedFrom = get("/api/v1/logs/groups/" + target + "/events", admin).getBody().at("/data/0");
        assertThat(mergedFrom.at("/type").asText()).isEqualTo("MERGED_FROM");
        assertThat(mergedFrom.at("/related_group_id").asLong()).isEqualTo(source);

        assertThat(exchange(HttpMethod.POST, "/api/v1/logs/groups/" + source + "/unmerge", editor.token(), null)
                .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        ResponseEntity<JsonNode> unmerged = exchange(HttpMethod.POST, "/api/v1/logs/groups/" + source + "/unmerge",
                admin, null);
        assertThat(unmerged.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(unmerged.getBody().at("/data/merged_into_id").isNull()).isTrue();
        assertThat(processEntryGroup(sourceMarker)).isEqualTo(source);
        assertThat(exchange(HttpMethod.POST, "/api/v1/logs/groups/" + source + "/unmerge", admin, null)
                .getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void mergesNeverChain() {
        String admin = adminToken();
        long a = process(marker());
        long b = process(marker());
        long c = process(marker());

        assertThat(merge(admin, a, a).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(merge(admin, c, a).getStatusCode()).isEqualTo(HttpStatus.OK);
        // c is merged into a; merging a into b re-points c to b.
        assertThat(merge(admin, a, b).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(jdbc.queryForObject("select merged_into_id from log_groups where id = ?", Long.class, c)).isEqualTo(b);
        // Nothing can be merged into a merged group.
        long d = process(marker());
        ResponseEntity<JsonNode> intoMerged = merge(admin, d, a);
        assertThat(intoMerged.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(intoMerged.getBody().at("/error/code").asText()).isEqualTo("INVALID_MERGE");
        assertThat(merge(admin, d, 999_999_999L).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    /* ── Helpers ────────────────────────────────────────────── */

    /** Creates a user through the admin API and completes its first-login password change. */
    private User user(String role) {
        String username = "collab-" + role.toLowerCase() + "-" + UUID.randomUUID().toString().substring(0, 8);
        ResponseEntity<JsonNode> created = exchange(HttpMethod.POST, "/api/v1/users", adminToken(),
                Map.of("username", username, "role", role, "password", INITIAL));
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String restricted = login(username, INITIAL).getBody().at("/data/token").asText();
        ResponseEntity<JsonNode> changed = exchange(HttpMethod.PUT, "/api/v1/auth/password", restricted,
                Map.of("current_password", INITIAL, "new_password", FINAL));
        assertThat(changed.getStatusCode()).isEqualTo(HttpStatus.OK);
        return new User(username, changed.getBody().at("/data/token").asText());
    }

    /** Letters only: digits are normalized away by the fingerprint, letters keep groups apart. */
    private static String marker() {
        StringBuilder marker = new StringBuilder("GroupCollaborationIT");
        for (int i = 0; i < 24; i++) {
            marker.append((char) ('a' + ThreadLocalRandom.current().nextInt(26)));
        }
        return marker.toString();
    }

    /** Processes one ERROR entry with the marker and returns the group it was stored in. */
    private long processEntryGroup(String marker) {
        String message = "java.lang.IllegalStateException: " + marker;
        String id = UUID.randomUUID().toString();
        processor.processBatch(List.of(LogEventDto.builder()
                .id(id)
                .source("it")
                .serviceName("collab-" + id)
                .severity(Severity.ERROR)
                .message(message)
                .rawLine(message)
                .timestamp(Instant.now())
                .build()));
        return jdbc.queryForObject("select group_id from log_entries where service_name = ?", Long.class,
                "collab-" + id);
    }

    /** Processes one entry of a new group and returns that group's id. */
    private long process(String marker) {
        processEntryGroup(marker);
        return jdbc.queryForObject("select id from log_groups where first_message like ?", Long.class,
                "%" + marker + "%");
    }

    private long count(long groupId) {
        return jdbc.queryForObject("select count from log_groups where id = ?", Long.class, groupId);
    }

    private ResponseEntity<JsonNode> put(String token, long groupId, Map<String, Object> body) {
        return exchange(HttpMethod.PUT, "/api/v1/logs/groups/" + groupId + "/assignee", token, body);
    }

    private ResponseEntity<JsonNode> merge(String token, long source, long target) {
        return exchange(HttpMethod.POST, "/api/v1/logs/groups/" + source + "/merge", token,
                Map.of("target_id", target));
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
}
