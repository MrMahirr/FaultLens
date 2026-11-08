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
            Map.entry("AnalysisResultDto", ApiContractIT.ANALYSIS),
            Map.entry("DeploymentDto", ApiContractIT.DEPLOYMENT),
            Map.entry("LogSourceDto", ApiContractIT.SOURCE),
            Map.entry("NotificationSettingsResponse", ApiContractIT.NOTIFICATION_SETTINGS),
            Map.entry("SystemSettingsResponse", ApiContractIT.SYSTEM_SETTINGS),
            Map.entry("ProfileResponse", ApiContractIT.PROFILE),
            Map.entry("TokenResponse", ApiContractIT.TOKEN),
            Map.entry("ProfileUpdateResponse", ApiContractIT.PROFILE_UPDATE));

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

    @Test
    void everyEndpointHasATypedResponse() throws IOException {
        // ApiResponse<?> publishes "ApiResponseObject": the frontend would get `unknown` data.
        assertThat(document().at("/components/schemas").propertyNames())
                .as("schemas")
                .doesNotContain("ApiResponseObject");
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
