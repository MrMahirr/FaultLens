package com.faultlens.collector.adapter;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.faultlens.collector.config.CollectorProperties;
import com.faultlens.collector.model.LogSource;
import com.faultlens.common.dto.LogEventDto;
import com.faultlens.common.enums.LogSourceType;
import com.faultlens.common.enums.Severity;
import com.faultlens.common.exception.CollectorException;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KubernetesAdapter implements LogSourceAdapter {
    private final CollectorProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Set<String> running = ConcurrentHashMap.newKeySet();

    /**
     * Returns Kubernetes source type.
     */
    @Override
    public LogSourceType getType() {
        return LogSourceType.KUBERNETES;
    }

    /**
     * Polls Kubernetes pod logs and emits each <b>new</b> line as a normalized event.
     *
     * <p>Logs are requested with timestamps; per pod the newest timestamp already emitted is kept
     * and the next poll asks only for lines since then ({@code sinceTime} has second precision, so
     * boundary lines are filtered by their own timestamp). The first poll of a pod resumes from the
     * source's {@code lastSeenAt}, so a backend restart does not replay old lines; a never-seen
     * source reads the existing log once. Pods that disappear are forgotten.
     */
    @Override
    // 3 attempts in total, as with spring-retry's default maxAttempts before the Framework 7 move.
    @Retryable(includes = CollectorException.class, maxRetries = 2, delay = 5000, multiplier = 2.0)
    public void startStreaming(LogSource source, Consumer<LogEventDto> onEvent) {
        String sourceId = String.valueOf(source.getId());
        running.add(sourceId);
        Map<String, String> config = parseConfig(source.getConfig());
        String namespace = config.getOrDefault("namespace", "default");
        Map<String, Instant> lastEmittedByPod = new HashMap<>();
        try (KubernetesClient client = createClient(config)) {
            while (running.contains(sourceId) && !Thread.currentThread().isInterrupted()) {
                Set<String> seenPods = new HashSet<>();
                for (Pod pod : client.pods().inNamespace(namespace).list().getItems()) {
                    String podName = pod.getMetadata().getName();
                    seenPods.add(podName);
                    Instant since = lastEmittedByPod.getOrDefault(podName, source.getLastSeenAt());
                    var podLog = client.pods().inNamespace(namespace).withName(podName).usingTimestamps();
                    // Always send sinceTime (epoch = whole log) so every poll is the same plain request.
                    String logs = podLog.sinceTime((since == null ? Instant.EPOCH : since).toString()).getLog();
                    for (TimedLine line : newLines(logs, since)) {
                        onEvent.accept(toEvent(source, config, pod, line));
                        if (line.timestamp() != null) {
                            lastEmittedByPod.merge(podName, line.timestamp(),
                                    (a, b) -> a.isAfter(b) ? a : b);
                        }
                    }
                }
                lastEmittedByPod.keySet().retainAll(seenPods);
                Thread.sleep(properties.getK8s().getReconnectIntervalSeconds() * 1000);
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (Exception ex) {
            throw new CollectorException("Kubernetes log streaming failed", "K8S_STREAM_FAILED", ex);
        } finally {
            running.remove(sourceId);
        }
    }

    /**
     * Stops Kubernetes streaming.
     */
    @Override
    public void stopStreaming(String sourceId) {
        running.remove(sourceId);
    }

    /**
     * Tests Kubernetes API access.
     */
    @Override
    public boolean testConnection(LogSource source) {
        try (KubernetesClient client = createClient(parseConfig(source.getConfig()))) {
            client.namespaces().list();
            return true;
        } catch (Exception ex) {
            log.warn("Kubernetes connection test failed for source {}", source.getId(), ex);
            return false;
        }
    }

    private KubernetesClient createClient(Map<String, String> config) throws Exception {
        if ("true".equalsIgnoreCase(config.getOrDefault("inCluster", "false"))) {
            return new KubernetesClientBuilder().build();
        }
        String kubeconfigPath = config.getOrDefault("kubeconfigPath",
                config.getOrDefault("kubeconfig", properties.getK8s().getKubeconfigPath()));
        Path resolved = Path.of(kubeconfigPath.replace("~", System.getProperty("user.home")));
        if (Files.exists(resolved)) {
            return new KubernetesClientBuilder().withConfig(Config.fromKubeconfig(Files.readString(resolved))).build();
        }
        return new KubernetesClientBuilder().withConfig(new ConfigBuilder().withNamespace(config.getOrDefault("namespace", "default")).build()).build();
    }

    /** A log line split from the RFC 3339 timestamp Kubernetes prefixes with {@code timestamps=true}. */
    record TimedLine(Instant timestamp, String text) {
    }

    /**
     * Splits a timestamped log and keeps the non-blank lines newer than {@code since}. A line
     * without a parseable timestamp is kept (and cannot advance the position).
     */
    static List<TimedLine> newLines(String logs, Instant since) {
        if (logs == null || logs.isBlank()) {
            return List.of();
        }
        List<TimedLine> result = new ArrayList<>();
        for (String raw : logs.split("\\R")) {
            if (raw.isBlank()) {
                continue;
            }
            int space = raw.indexOf(' ');
            Instant timestamp = null;
            String text = raw;
            if (space > 0) {
                try {
                    timestamp = Instant.parse(raw.substring(0, space));
                    text = raw.substring(space + 1);
                } catch (DateTimeParseException ignored) {
                    // not a timestamp prefix: keep the whole line
                }
            }
            if (text.isBlank() || (timestamp != null && since != null && !timestamp.isAfter(since))) {
                continue;
            }
            result.add(new TimedLine(timestamp, text));
        }
        return result;
    }

    private LogEventDto toEvent(LogSource source, Map<String, String> config, Pod pod, TimedLine timedLine) {
        String line = timedLine.text();
        Map<String, String> labels = new java.util.HashMap<>(
                pod.getMetadata().getLabels() == null ? Map.of() : pod.getMetadata().getLabels());
        labels.put("sourceId", String.valueOf(source.getId()));
        return LogEventDto.builder()
                .id(UUID.randomUUID().toString())
                .source("kubernetes")
                .cluster(config.getOrDefault("cluster", "default"))
                .namespace(pod.getMetadata().getNamespace())
                .podName(pod.getMetadata().getName())
                .containerName(config.get("containerName"))
                .nodeName(pod.getSpec() == null ? null : pod.getSpec().getNodeName())
                .serviceName(labels.getOrDefault("app", source.getName()))
                .severity(Severity.INFO)
                .message(line)
                .rawLine(line)
                .timestamp(timedLine.timestamp() == null ? Instant.now() : timedLine.timestamp())
                .labels(labels)
                .build();
    }

    private Map<String, String> parseConfig(String json) {
        try {
            if (json == null || json.isBlank()) {
                return Map.of();
            }
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception ex) {
            throw new CollectorException("Invalid Kubernetes source config", "INVALID_SOURCE_CONFIG", ex);
        }
    }
}
