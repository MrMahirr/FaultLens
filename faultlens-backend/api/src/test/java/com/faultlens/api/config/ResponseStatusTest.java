package com.faultlens.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * A handler returning {@code ResponseEntity<Void>} answers 204 here (delete, revoke, logout).
 * Without {@code @ResponseStatus(NO_CONTENT)} the OpenAPI contract would document it as 200, so
 * a new one cannot forget it.
 */
class ResponseStatusTest {
    @Test
    void bodylessHandlersDocument204() throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));

        List<String> missing = new ArrayList<>();
        int checked = 0;
        for (BeanDefinition definition : scanner.findCandidateComponents("com.faultlens")) {
            for (Method method : Class.forName(definition.getBeanClassName()).getDeclaredMethods()) {
                if (!AnnotatedElementUtils.hasAnnotation(method, RequestMapping.class) || !returnsVoidEntity(method)) {
                    continue;
                }
                checked++;
                ResponseStatus status = method.getAnnotation(ResponseStatus.class);
                if (status == null || status.value() != HttpStatus.NO_CONTENT) {
                    missing.add(method.getDeclaringClass().getSimpleName() + "." + method.getName());
                }
            }
        }
        assertThat(checked).as("ResponseEntity<Void> handlers found").isPositive();
        assertThat(missing).as("ResponseEntity<Void> handlers without @ResponseStatus(NO_CONTENT)").isEmpty();
    }

    private static boolean returnsVoidEntity(Method method) {
        return method.getReturnType() == ResponseEntity.class
                && method.getGenericReturnType() instanceof ParameterizedType type
                && type.getActualTypeArguments()[0] == Void.class;
    }
}
