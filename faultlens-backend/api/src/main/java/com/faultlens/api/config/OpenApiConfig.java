package com.faultlens.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.core.util.Json31;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import com.faultlens.api.dto.ErrorEnvelope;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.servers.Server;
import com.faultlens.api.security.ApiKeyAuthFilter;
import com.faultlens.api.security.ApiKeyScope;
import java.util.Arrays;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI document at {@code /v3/api-docs}. It is the API contract: {@code contracts/openapi.json}
 * is generated from it (OpenApiContractIT) and the frontend types from that file (P10 / 1.3).
 */
@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI faultlensOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("faultlens API")
                        .version("1.0.0")
                        .description("faultlens - Real-time distant server log analysis and anomaly correlation platform API description."))
                // Fixed, relative server: otherwise springdoc writes the request's host and port,
                // which would make the generated contract differ between machines and runs.
                .servers(List.of(new Server().url("/").description("This FaultLens instance")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER))
                .components(new Components()
                        .addSecuritySchemes(BEARER, new SecurityScheme()
                                .description("User access token (JWT, 15 min) from POST /api/v1/auth/login")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }

    /**
     * Documents property names as they are on the wire.
     *
     * <p>swagger-core introspects models with Jackson 2, so it does not see the Jackson 3
     * {@code @JsonNaming(SnakeCaseStrategy)} on our DTOs and would publish the Java (camelCase)
     * names. {@link JacksonNamingIntrospector} reads the Jackson 3 annotation per class, as the
     * real mapper does: a DTO without it is documented with its Java names, so a forgotten
     * annotation shows in the contract instead of being papered over (DtoNamingTest also fails
     * on it). Explicit {@code @JsonProperty} names still win.
     */
    @Bean
    public ModelResolver wireNamingModelResolver() {
        // The document is OpenAPI 3.1 (springdoc's default), so the resolver must emit 3.1
        // schemas too (type arrays instead of `nullable`, numeric exclusiveMinimum…), as
        // springdoc's own resolver does: Json31 mapper + openapi31(true).
        ObjectMapper mapper = Json31.mapper().copy();
        mapper.setAnnotationIntrospector(AnnotationIntrospector.pair(
                new JacksonNamingIntrospector(), mapper.getSerializationConfig().getAnnotationIntrospector()));
        return new ModelResolver(mapper).openapi31(true);
    }

    /**
     * Stable operation ids: {@code <controller><Method>}, e.g. {@code sourceList},
     * {@code logGroupEntries}, {@code apiKeyRevoke}.
     *
     * <p>springdoc otherwise uses the bare method name and numbers duplicates in discovery order
     * ({@code list_7}, {@code create_4}): adding a controller renumbered unrelated operations, so
     * the contract and the generated TypeScript churned and {@code operations["list_7"]} could
     * silently point at another endpoint. The controller prefix is skipped when the method name
     * already starts with it ({@code IngestController.ingestJson} → {@code ingestJson}), and an
     * explicit {@code @Operation(operationId = ...)} is kept. OpenApiContractIT fails on a
     * duplicate id instead of letting a suffix appear.
     */
    @Bean
    public OperationCustomizer stableOperationIds() {
        return (operation, handlerMethod) -> {
            Operation explicit = handlerMethod.getMethodAnnotation(Operation.class);
            if (explicit == null || explicit.operationId().isEmpty()) {
                // getUserClass: controllers with @PreAuthorize are CGLIB proxies (Foo$$SpringCGLIB$$0).
                Class<?> controller = ClassUtils.getUserClass(handlerMethod.getBeanType());
                operation.setOperationId(operationId(controller, handlerMethod.getMethod().getName()));
            }
            return operation;
        };
    }

    static String operationId(Class<?> controller, String method) {
        String prefix = StringUtils.uncapitalize(controller.getSimpleName().replaceFirst("Controller$", ""));
        return method.startsWith(prefix) ? method : prefix + StringUtils.capitalize(method);
    }

    // Component names may only use [A-Za-z0-9._-] (OpenAPI 3.1); the old "Bearer Authentication"
    // was invalid.
    static final String BEARER = "bearerAuth";
    static final String API_KEY = "apiKey";

    /**
     * Which credential each operation takes, derived from {@link ApiKeyScope} (the rules the
     * ApiKeyAuthFilter enforces), so the contract cannot drift from them:
     * <ul>
     *   <li>logs:ingest endpoints: an API key only (a user token is refused with 403);</li>
     *   <li>endpoints another scope covers (POST /deployments, GET /logs…, GET /alarms…): a user
     *       token or an API key with that scope;</li>
     *   <li>everything else: a user token (the document-wide default).</li>
     * </ul>
     * Scope names are listed as the API key requirement's roles (allowed by OpenAPI 3.1).
     */
    @Bean
    public OpenApiCustomizer apiKeySecurity() {
        return openApi -> {
            openApi.getComponents().addSecuritySchemes(API_KEY, new SecurityScheme()
                    .type(SecurityScheme.Type.APIKEY)
                    .in(SecurityScheme.In.HEADER)
                    .name(ApiKeyAuthFilter.HEADER)
                    .description("FaultLens API key (flk_…), created under Settings > API keys. "
                            + "`Authorization: Bearer flk_…` is accepted too. A key only reaches the "
                            + "endpoints of its scopes."));
            openApi.getPaths().forEach((path, item) -> item.readOperationsMap().forEach((method, operation) -> {
                List<String> scopes = Arrays.stream(ApiKeyScope.values())
                        .filter(scope -> scope.allows(method.name(), path))
                        .map(ApiKeyScope::value)
                        .toList();
                if (scopes.isEmpty()) {
                    return;
                }
                SecurityRequirement apiKey = new SecurityRequirement().addList(API_KEY, scopes);
                operation.setSecurity(scopes.equals(List.of(ApiKeyScope.LOGS_INGEST.value()))
                        ? List.of(apiKey)
                        : List.of(new SecurityRequirement().addList(BEARER), apiKey));
            }));
        };
    }

    /**
     * Ingest request bodies the handler annotations cannot express:
     * <ul>
     *   <li>POST /api/v1/ingest/logs takes one record, an array of records or {"logs": [...]}
     *       (JsonLogDecoder); the annotation can only name the single record;</li>
     *   <li>POST /v1/logs has two handlers (protobuf and JSON) that springdoc merges into one
     *       operation, keeping only one request content: both encodings are listed.</li>
     * </ul>
     */
    @Bean
    public OpenApiCustomizer ingestBodies() {
        return openApi -> {
            PathItem otlp = openApi.getPaths().get("/v1/logs");
            if (otlp != null && otlp.getPost() != null && otlp.getPost().getRequestBody() != null) {
                otlp.getPost().getRequestBody()
                        .description("OTLP ExportLogsServiceRequest, protobuf or JSON encoding")
                        .getContent()
                        .addMediaType("application/x-protobuf", new io.swagger.v3.oas.models.media.MediaType()
                                .schema(new Schema<>().type("string").format("binary")));
            }
            PathItem item = openApi.getPaths().get("/api/v1/ingest/logs");
            if (item == null || item.getPost() == null || item.getPost().getRequestBody() == null) {
                return;
            }
            io.swagger.v3.oas.models.media.MediaType json =
                    item.getPost().getRequestBody().getContent().get(org.springframework.http.MediaType.APPLICATION_JSON_VALUE);
            Schema<?> record = new Schema<>().$ref("#/components/schemas/IngestLogRequest");
            json.setSchema(new Schema<>().oneOf(List.of(
                    record,
                    new ArraySchema().items(record),
                    new ObjectSchema().addProperty("logs", new ArraySchema().items(record)).addRequiredItem("logs"))));
        };
    }

    /**
     * Every failure answers the same envelope, {@code {"success": false, "error": {"code",
     * "message", "timestamp"}}} (GlobalExceptionHandler, the security filters). It is documented
     * once per operation as the {@code default} response: which 4xx/5xx an endpoint can return is
     * not listed (that would claim, e.g., a 404 on list endpoints), only what an error looks like,
     * which is what clients parse ({@code error.code}).
     */
    @Bean
    public OpenApiCustomizer errorEnvelope(ModelResolver wireNamingModelResolver) {
        return openApi -> {
            ModelConverters converters = new ModelConverters(true);
            converters.addConverter(wireNamingModelResolver);
            converters.readAll(new AnnotatedType(ErrorEnvelope.class)).forEach(openApi.getComponents()::addSchemas);

            ApiResponse error = new ApiResponse()
                    .description("Error: the status says what failed, `error.code` why (e.g. NOT_FOUND, "
                            + "API_KEY_SCOPE, INVALID_API_KEY)")
                    .content(new Content().addMediaType(org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
                            new io.swagger.v3.oas.models.media.MediaType()
                                    .schema(new Schema<>().$ref("#/components/schemas/ErrorEnvelope"))));
            openApi.getPaths().values().forEach(item -> item.readOperations()
                    .forEach(operation -> operation.getResponses().addApiResponse("default", error)));
        };
    }
}
