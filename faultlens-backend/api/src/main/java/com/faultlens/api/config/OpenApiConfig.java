package com.faultlens.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
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
     * Documents property names in snake_case, as they are on the wire.
     *
     * <p>swagger-core introspects models with Jackson 2, so it does not see the Jackson 3
     * {@code @JsonNaming(SnakeCaseStrategy)} on our DTOs and would publish camelCase names. Every
     * DTO is snake_case (those without the annotation only have one-word fields), so the naming
     * strategy is applied to the whole document; explicit {@code @JsonProperty} names still win.
     * OpenApiContractIT checks the result against the real JSON of the main endpoints.
     */
    @Bean
    public ModelResolver snakeCaseModelResolver() {
        ObjectMapper mapper = Json.mapper().copy().setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        return new ModelResolver(mapper);
    }
}
