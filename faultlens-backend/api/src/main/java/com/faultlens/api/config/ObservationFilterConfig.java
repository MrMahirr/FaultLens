package com.faultlens.api.config;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationPredicate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.observation.ServerRequestObservationContext;

/**
 * Keeps traces about requests and the log pipeline (P08).
 *
 * <p>Only an HTTP request (not actuator / probes) or a Kafka send/receive may start a trace;
 * anything else is recorded only inside one. Without this, housekeeping dominated: the collector's
 * 250 ms flush, lease heartbeats (Redis commands) and the security filters of every Prometheus
 * scrape each started their own trace. Metrics are unaffected for HTTP; other observations
 * without a parent are simply not recorded.
 */
@Configuration
public class ObservationFilterConfig {
    @Bean
    ObservationPredicate onlyRequestsAndPipelineStartTraces() {
        return (name, context) -> {
            if (context instanceof ServerRequestObservationContext request) {
                String path = request.getCarrier().getRequestURI();
                return !(path.startsWith("/actuator") || "/livez".equals(path) || "/readyz".equals(path));
            }
            // A suppressed observation still becomes a (no-op) parent: treat it as none.
            var parent = context.getParentObservation();
            boolean realParent = parent != null && !(parent instanceof Observation observation && observation.isNoop());
            return realParent || isKafka(context);
        };
    }

    /**
     * Second line, at export: a span without a parent is exported only if it is an HTTP request or
     * a Kafka send/receive (children of suppressed observations surface as such roots).
     */
    @Bean
    io.micrometer.tracing.exporter.SpanExportingPredicate onlyRequestAndPipelineRootSpans() {
        return span -> {
            String parent = span.getParentId();
            boolean root = parent == null || parent.isBlank() || parent.chars().allMatch(c -> c == '0');
            if (!root) {
                return true;
            }
            String name = span.getName() == null ? "" : span.getName();
            return name.startsWith("http ") || name.contains(" send") || name.contains(" receive")
                    || name.startsWith("log-") || span.getKind() == io.micrometer.tracing.Span.Kind.PRODUCER
                    || span.getKind() == io.micrometer.tracing.Span.Kind.CONSUMER;
        };
    }

    private static boolean isKafka(Observation.Context context) {
        return context.getClass().getName().startsWith("org.springframework.kafka.");
    }
}
