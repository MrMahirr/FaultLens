package com.faultlens.api.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import tools.jackson.core.util.DefaultIndenter;
import tools.jackson.core.util.DefaultPrettyPrinter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.json.JsonMapper;

/**
 * The API contract (P10 / 1.3): {@code /v3/api-docs} must equal the committed
 * {@code contracts/openapi.json}, from which the frontend generates its TypeScript types
 * ({@code pnpm gen:api}). A controller or DTO change that alters the API fails here until the
 * contract is regenerated, so the frontend can never silently drift from the backend.
 *
 * <p>Regenerate after an intended change:
 * <pre>./mvnw verify -DskipUTs -pl api -am -Dit.test=OpenApiContractIT -Dcontracts.update=true</pre>
 * then {@code pnpm gen:api} in faultlens-frontend, and commit both files.
 *
 * <p>The document is also checked against the real JSON of the main endpoints (the field sets
 * pinned by {@link ApiContractIT}): the schemas must use the wire names, not the Java ones.
 */
class OpenApiContractIT extends AbstractIntegrationTest {
    /** The IT runs in faultlens-backend/api; the contract lives at the repository root. */
    private static final Path CONTRACT = Path.of("..", "..", "contracts", "openapi.json");
    private static final Path GENERATED = Path.of("target", "openapi.json");

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    /**
     * Jackson's default pretty printer, but with an LF line feed instead of the system line
     * separator: on Windows INDENT_OUTPUT writes CRLF, so the contract regenerated there differed
     * byte for byte from one generated on Linux (and mixed CRLF with the final LF). Arrays stay
     * on one line, as with the default printer.
     */
    private static final ObjectWriter PRETTY = MAPPER.writer()
            .with(new DefaultPrettyPrinter().withObjectIndenter(new DefaultIndenter("  ", "\n")));

    /** OpenAPI component schema → the JSON field set ApiContractIT pins for it. */
    private static final Map<String, Set<String>> WIRE_FIELDS = Map.ofEntries(
            Map.entry("AlarmDto", ApiContractIT.ALARM),
            Map.entry("AlarmStatsDto", ApiContractIT.ALARM_STATS),
            Map.entry("LogEntryDto", ApiContractIT.LOG_ENTRY),
            Map.entry("LogGroupDto", ApiContractIT.LOG_GROUP),
            Map.entry("LogGroupEventDto", ApiContractIT.LOG_GROUP_EVENT),
            Map.entry("LogGroupCommentDto", ApiContractIT.LOG_GROUP_COMMENT),
            Map.entry("AnalysisResultDto", ApiContractIT.ANALYSIS),
            Map.entry("DeploymentDto", ApiContractIT.DEPLOYMENT),
            Map.entry("LogSourceDto", ApiContractIT.SOURCE),
            Map.entry("NotificationSettingsResponse", ApiContractIT.NOTIFICATION_SETTINGS),
            Map.entry("SystemSettingsResponse", ApiContractIT.SYSTEM_SETTINGS),
            Map.entry("ProfileResponse", ApiContractIT.PROFILE),
            Map.entry("TokenResponse", ApiContractIT.TOKEN),
            Map.entry("ProfileUpdateResponse", ApiContractIT.PROFILE_UPDATE),
            Map.entry("ErrorEnvelope", ApiContractIT.ERROR_ENVELOPE),
            Map.entry("ErrorBody", ApiContractIT.ERROR_BODY));

    @Test
    void schemasUseTheWireFieldNames() throws IOException {
        JsonNode schemas = document().at("/components/schemas");
        WIRE_FIELDS.forEach((schema, fields) -> assertThat(schemas.at("/" + schema + "/properties").propertyNames())
                .as("properties of schema %s", schema)
                .containsExactlyInAnyOrderElementsOf(fields));
        assertThat(schemas.at("/PagedResponseLogEntryDto/properties").propertyNames())
                .as("page envelope")
                .containsExactlyInAnyOrderElementsOf(ApiContractIT.PAGE);
    }

    /**
     * Responses that are untyped on purpose. OTLP/HTTP answers an ExportLogsServiceResponse; with
     * no partial_success it is the empty object the protocol defines, not data for the UI.
     */
    private static final Set<String> UNTYPED_ALLOWED = Set.of("ingestOtlpLogs application/json");

    @Test
    void everyEndpointHasATypedResponse() throws IOException {
        JsonNode document = document();
        List<String> untyped = new ArrayList<>();
        document.at("/paths").forEach(path -> path.forEach(operation -> {
            String operationId = operation.at("/operationId").asText();
            operation.at("/responses").forEach(response -> response.at("/content").propertyNames().forEach(media -> {
                if (!UNTYPED_ALLOWED.contains(operationId + " " + media)) {
                    collectUntyped(document, response.at("/content/" + escape(media) + "/schema"),
                            operationId + " " + media, new java.util.HashSet<>(), untyped);
                }
            }));
        }));
        // An untyped object is `unknown` (or `Record<string, never>`) in the generated TypeScript:
        // ApiResponse<?>, Object, Map<String, Object> (e.g. ApiResponseObject).
        assertThat(untyped).as("untyped response schemas").isEmpty();
    }

    /** Walks a schema (following $refs once each) and records objects with no declared shape. */
    private static void collectUntyped(JsonNode document, JsonNode schema, String where, Set<String> seen,
                                       List<String> untyped) {
        if (schema.isMissingNode() || schema.isNull()) {
            return;
        }
        if (schema.has("$ref")) {
            String ref = schema.at("/$ref").asText();
            if (seen.add(ref)) {
                collectUntyped(document, document.at(ref.substring(1)), where + " > " + ref.substring(ref.lastIndexOf('/') + 1),
                        seen, untyped);
            }
            return;
        }
        schema.at("/properties").forEach(property -> collectUntyped(document, property, where, seen, untyped));
        collectUntyped(document, schema.at("/items"), where + "[]", seen, untyped);
        JsonNode additional = schema.at("/additionalProperties");
        if (additional.isObject() && !additional.isEmpty()) {
            collectUntyped(document, additional, where + "{}", seen, untyped);
        }

        boolean object = schema.at("/type").asText("object").equals("object") && !schema.has("items")
                && !schema.has("oneOf") && !schema.has("anyOf") && !schema.has("allOf") && !schema.has("enum");
        boolean shapeless = !schema.has("properties")
                && (additional.isMissingNode() || additional.isBoolean() || additional.isEmpty());
        if (object && shapeless && !schema.has("format")) {
            untyped.add(where);
        }
    }

    /** JSON Pointer escaping for a media type such as "application/json". */
    private static String escape(String token) {
        return token.replace("~", "~0").replace("/", "~1");
    }

    /**
     * Success status per operation, where it is not 200. The handlers set it on their
     * ResponseEntity; @ResponseStatus on the handler is what makes the document say so (the real
     * status is pinned by the endpoint ITs).
     */
    private static final Map<String, String> SUCCESS_STATUS = Map.ofEntries(
            Map.entry("alarmRuleCreate", "201"),
            Map.entry("apiKeyCreate", "201"),
            Map.entry("sourceCreate", "201"),
            Map.entry("userCreate", "201"),
            Map.entry("logAddGroupComment", "201"),
            Map.entry("ingestJson", "202"),
            Map.entry("alarmDelete", "204"),
            Map.entry("alarmRuleDelete", "204"),
            Map.entry("apiKeyRevoke", "204"),
            Map.entry("authLogout", "204"),
            Map.entry("logDeleteGroupComment", "204"),
            Map.entry("sourceDelete", "204"),
            Map.entry("userDelete", "204"));

    @Test
    void documentsTheRealSuccessStatus() throws IOException {
        Map<String, List<String>> successCodes = new java.util.HashMap<>();
        document().at("/paths").forEach(path -> path.forEach(operation -> {
            List<String> codes = new ArrayList<>();
            operation.at("/responses").propertyNames().forEach(code -> {
                if (code.startsWith("2")) {
                    codes.add(code);
                }
            });
            successCodes.put(operation.at("/operationId").asText(), codes);
        }));
        successCodes.forEach((operationId, codes) -> assertThat(codes)
                .as("success responses of %s", operationId)
                .containsExactly(SUCCESS_STATUS.getOrDefault(operationId, "200")));
    }

    @Test
    void documentsWhichCredentialEachOperationTakes() throws IOException {
        JsonNode paths = document().at("/paths");
        // Ingest: API key only (user tokens get 403).
        assertThat(paths.at("/~1api~1v1~1ingest~1logs/post/security").toString())
                .isEqualTo("[{\"apiKey\":[\"logs:ingest\"]}]");
        assertThat(paths.at("/~1v1~1logs/post/security").toString())
                .isEqualTo("[{\"apiKey\":[\"logs:ingest\"]}]");
        // Scoped endpoints: a user token or a key with the scope.
        assertThat(paths.at("/~1api~1v1~1deployments/post/security").toString())
                .isEqualTo("[{\"bearerAuth\":[]},{\"apiKey\":[\"deployments:write\"]}]");
        assertThat(paths.at("/~1api~1v1~1alarms/get/security").toString())
                .isEqualTo("[{\"bearerAuth\":[]},{\"apiKey\":[\"alarms:read\"]}]");
        // Others: the document-wide user token.
        assertThat(paths.at("/~1api~1v1~1sources/get/security").isMissingNode()).isTrue();
    }

    @Test
    void documentsTheIngestBodies() throws IOException {
        JsonNode paths = document().at("/paths");
        assertThat(paths.at("/~1api~1v1~1ingest~1logs/post/requestBody/content/application~1json/schema/oneOf").size())
                .as("single record, array, {logs: [...]}")
                .isEqualTo(3);
        assertThat(paths.at("/~1v1~1logs/post/requestBody/content").propertyNames())
                .containsExactlyInAnyOrder("application/json", "application/x-protobuf");
    }

    @Test
    void everyOperationDocumentsTheErrorEnvelope() throws IOException {
        List<String> missing = new ArrayList<>();
        document().at("/paths").forEach(path -> path.forEach(operation -> {
            if (!operation.at("/responses/default/content/application~1json/schema/$ref").asText()
                    .equals("#/components/schemas/ErrorEnvelope")) {
                missing.add(operation.at("/operationId").asText());
            }
        }));
        assertThat(missing).as("operations without the error envelope").isEmpty();
    }

    @Test
    void operationIdsAreStableAndUnique() throws IOException {
        List<String> ids = new ArrayList<>();
        document().at("/paths").forEach(path -> path.forEach(operation -> {
            if (operation.has("operationId")) {
                ids.add(operation.at("/operationId").asText());
            }
        }));
        assertThat(ids).as("operation ids").isNotEmpty().doesNotHaveDuplicates();
        // A numeric suffix (list_7) means springdoc deduplicated a clash: positional, so unstable.
        assertThat(ids).as("operation ids with a positional suffix").noneMatch(id -> id.matches(".*_[0-9]+"));
    }

    @Test
    void matchesTheCommittedContract() throws IOException {
        JsonNode actual = document();
        String rendered = PRETTY.writeValueAsString(actual) + "\n";
        Files.createDirectories(GENERATED.getParent());
        Files.writeString(GENERATED, rendered, StandardCharsets.UTF_8);

        if (Boolean.getBoolean("contracts.update")) {
            Files.createDirectories(CONTRACT.getParent());
            Files.writeString(CONTRACT, rendered, StandardCharsets.UTF_8);
            return;
        }

        assertThat(CONTRACT)
                .as("contracts/openapi.json is missing; run this IT with -Dcontracts.update=true")
                .exists();
        JsonNode committed = MAPPER.readTree(Files.readString(CONTRACT, StandardCharsets.UTF_8));
        assertThat(actual)
                .as("""
                        The API changed but contracts/openapi.json did not. Compare it with
                        api/target/openapi.json; if the change is intended, rerun with
                        -Dcontracts.update=true, run `pnpm gen:api` in faultlens-frontend and commit
                        both files.""")
                .isEqualTo(committed);
    }

    private JsonNode document() {
        ResponseEntity<JsonNode> response = rest.getForEntity("/v3/api-docs", JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }
}
