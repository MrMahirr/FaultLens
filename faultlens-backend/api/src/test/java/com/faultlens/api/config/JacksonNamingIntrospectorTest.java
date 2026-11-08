package com.faultlens.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.faultlens.api.dto.AlarmDto;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.media.Schema;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;
import tools.jackson.databind.json.JsonMapper;

/**
 * The OpenAPI model resolver must name properties exactly like the Jackson 3 mapper that writes
 * the JSON: per class, from the Jackson 3 {@code @JsonNaming}.
 */
class JacksonNamingIntrospectorTest {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    record Snake(String retryCount, int maxValue) {}

    /** No @JsonNaming: the mapper writes the Java names, so the document must too. */
    record Plain(String retryCount) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    record Explicit(@JsonProperty("customName") String retryCount, String otherField) {}

    @JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
    record Kebab(String retryCount) {}

    private static Map<String, Schema> resolve(Class<?> type) {
        ModelConverters converters = new ModelConverters();
        converters.addConverter(new OpenApiConfig().wireNamingModelResolver());
        return converters.readAll(new AnnotatedType(type));
    }

    private static Iterable<String> properties(Class<?> type) {
        return resolve(type).get(type.getSimpleName()).getProperties().keySet();
    }

    @Test
    void appliesTheJackson3NamingOfTheClass() {
        assertThat(properties(Snake.class)).containsExactlyInAnyOrder("retry_count", "max_value");
    }

    @Test
    void keepsJavaNamesWithoutJsonNaming() {
        assertThat(properties(Plain.class)).containsExactly("retryCount");
    }

    @Test
    void explicitJsonPropertyWins() {
        assertThat(properties(Explicit.class)).containsExactlyInAnyOrder("customName", "other_field");
    }

    @Test
    void delegatesToTheDeclaredStrategy() {
        assertThat(properties(Kebab.class)).containsExactly("retry-count");
    }

    @Test
    void realDtoMatchesItsJson() throws Exception {
        AlarmDto alarm = new AlarmDto();
        Iterable<String> json = JsonMapper.builder().build()
                .readTree(JsonMapper.builder().build().writeValueAsString(alarm)).propertyNames();
        assertThat(properties(AlarmDto.class)).containsExactlyInAnyOrderElementsOf(json);
    }
}
