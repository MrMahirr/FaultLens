package com.faultlens.api.config;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.NopAnnotationIntrospector;
import java.lang.reflect.Constructor;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * Makes swagger-core honour the Jackson 3 {@code @JsonNaming} our DTOs use.
 *
 * <p>swagger-core (and so springdoc) introspects models with Jackson 2, which only knows its own
 * {@code com.fasterxml.jackson.databind.annotation.JsonNaming}; the Jackson 3 annotation
 * ({@code tools.jackson.databind.annotation.JsonNaming}) is invisible to it, so the OpenAPI
 * document would show the Java (camelCase) names while the API sends snake_case.
 *
 * <p>This introspector answers Jackson 2's "which naming strategy does this class use?" from the
 * Jackson 3 annotation, per class, exactly like the real (Jackson 3) mapper decides: a class with
 * {@code @JsonNaming} gets that strategy, a class without keeps its Java names. The Jackson 3
 * strategy itself does the renaming (see {@link Jackson3Naming}), so any strategy, including a
 * custom one, is published as it is applied on the wire.
 */
public class JacksonNamingIntrospector extends NopAnnotationIntrospector {
    @Override
    public Object findNamingStrategy(AnnotatedClass ac) {
        JsonNaming naming = ac.getAnnotated().getAnnotation(JsonNaming.class);
        return naming == null ? null : new Jackson3Naming(instantiate(naming.value(), ac));
    }

    private static tools.jackson.databind.PropertyNamingStrategy instantiate(
            Class<? extends tools.jackson.databind.PropertyNamingStrategy> type, AnnotatedClass ac) {
        try {
            Constructor<? extends tools.jackson.databind.PropertyNamingStrategy> constructor =
                    type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            // Fail the document instead of silently publishing the Java names.
            throw new IllegalStateException("Cannot instantiate naming strategy " + type.getName()
                    + " declared by @JsonNaming on " + ac.getRawType().getName(), e);
        }
    }

    /**
     * A Jackson 2 strategy that renames with the Jackson 3 one. Only name translation is needed
     * (that is all the OpenAPI document shows), and Jackson 3's built-in strategies derive every
     * accessor name from the property name alone, ignoring the config and member arguments.
     */
    static final class Jackson3Naming extends PropertyNamingStrategies.NamingBase {
        private final tools.jackson.databind.PropertyNamingStrategy delegate;

        Jackson3Naming(tools.jackson.databind.PropertyNamingStrategy delegate) {
            if (!(delegate instanceof tools.jackson.databind.PropertyNamingStrategies.NamingBase)) {
                // A custom strategy may look at the member or config, which a Jackson 2 call
                // cannot provide; refuse rather than guess.
                throw new IllegalStateException("Only PropertyNamingStrategies.NamingBase strategies are "
                        + "supported for the OpenAPI document, got " + delegate.getClass().getName());
            }
            this.delegate = delegate;
        }

        @Override
        public String translate(String propertyName) {
            return delegate.nameForField(null, null, propertyName);
        }
    }
}
