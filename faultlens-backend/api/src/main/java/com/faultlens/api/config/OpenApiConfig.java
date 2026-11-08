package com.faultlens.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
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
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication", new SecurityScheme()
                                .name("Bearer Authentication")
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
        ObjectMapper mapper = Json.mapper().copy();
        mapper.setAnnotationIntrospector(AnnotationIntrospector.pair(
                new JacksonNamingIntrospector(), mapper.getSerializationConfig().getAnnotationIntrospector()));
        return new ModelResolver(mapper);
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
}
