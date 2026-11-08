package com.faultlens.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.RegexPatternTypeFilter;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * Every DTO is snake_case on the wire, and the frontend converts snake_case to camelCase. A DTO
 * with a multi-word field but no Jackson 3 {@code @JsonNaming} would be written as camelCase
 * (e.g. {@code retryCount}): the frontend would read and send the wrong key and the value would be
 * silently dropped. The OpenAPI contract documents such a DTO as it really is (camelCase, see
 * JacksonNamingIntrospector), and this test fails on it before it reaches the contract.
 *
 * <p>Scans the main classes of all modules for {@code *Dto}, {@code *Request} and
 * {@code *Response} types (nested ones included).
 */
class DtoNamingTest {
    private static final Pattern DTO_NAME = Pattern.compile("com\\.faultlens\\..*(Dto|Request|Response)");

    /**
     * Intentional camelCase JSON (not part of the API contract). Add an entry only with a reason;
     * it is written next to the class name.
     */
    private static final Set<String> ALLOWED = Set.of();

    @Test
    void multiWordDtoFieldsAreSnakeCaseOnTheWire() throws ClassNotFoundException {
        List<String> violations = new ArrayList<>();
        for (Class<?> type : dtoClasses()) {
            if (ALLOWED.contains(type.getName()) || type.isAnnotationPresent(JsonNaming.class)) {
                continue;
            }
            for (Field field : type.getDeclaredFields()) {
                if (isMultiWord(field) && !hasExplicitName(field)) {
                    violations.add(type.getName() + "." + field.getName());
                }
            }
        }
        assertThat(violations)
                .as("Multi-word fields that would be camelCase in JSON. Add "
                        + "@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class) "
                        + "(tools.jackson.databind.annotation) to the class, or @JsonProperty to the field")
                .isEmpty();
    }

    @Test
    void scanFindsTheDtos() throws ClassNotFoundException {
        // Guards the scan itself: a broken pattern would make the main test pass vacuously.
        assertThat(dtoClasses()).extracting(Class::getName).contains(
                "com.faultlens.api.dto.AlarmDto",
                "com.faultlens.common.dto.LogEntryDto",
                "com.faultlens.collector.dto.LogSourceCreateRequest",
                "com.faultlens.api.dto.AlarmRuleDtos$AlarmRuleRequest");
    }

    private static List<Class<?>> dtoClasses() throws ClassNotFoundException {
        // Every class, nested ones included (the default only accepts top-level components).
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false) {
            @Override
            protected boolean isCandidateComponent(AnnotatedBeanDefinition definition) {
                return true;
            }
        };
        scanner.addIncludeFilter(new RegexPatternTypeFilter(DTO_NAME));

        List<Class<?>> classes = new ArrayList<>();
        for (BeanDefinition definition : scanner.findCandidateComponents("com.faultlens")) {
            Class<?> type = Class.forName(definition.getBeanClassName());
            if (isMainClass(type) && !type.isInterface() && !type.isEnum()) {
                classes.add(type);
            }
        }
        return classes;
    }

    /** Test fixtures (e.g. JacksonNamingIntrospectorTest's camelCase records) are not API DTOs. */
    private static boolean isMainClass(Class<?> type) {
        var source = type.getProtectionDomain().getCodeSource();
        return source != null && !source.getLocation().toString().contains("test-classes");
    }

    private static boolean isMultiWord(Field field) {
        return !Modifier.isStatic(field.getModifiers())
                && !field.isSynthetic()
                && !field.getName().equals(field.getName().toLowerCase());
    }

    private static boolean hasExplicitName(Field field) {
        JsonProperty property = field.getAnnotation(JsonProperty.class);
        return property != null && !property.value().isEmpty();
    }
}
