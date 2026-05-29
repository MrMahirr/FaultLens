package com.faultlens.collector.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
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
     * Polls Kubernetes pod logs and emits each line as a normalized event.
     */
    @Override
    @Retryable(retryFor = CollectorException.class, backoff = @Backoff(delay = 5000, multiplier = 2.0))
    public void startStreaming(LogSource source, Consumer<LogEventDto> onEvent) {
        String sourceId = String.valueOf(source.getId());
        running.add(sourceId);
        Map<String, String> config = parseConfig(source.getConfig());
        String namespace = config.getOrDefault("namespace", "default");
        try (KubernetesClient client = createClient(config)) {
            while (running.contains(sourceId) && !Thread.currentThread().isInterrupted()) {
                for (Pod pod : client.pods().inNamespace(namespace).list().getItems()) {
                    String podName = pod.getMetadata().getName();
                    String logs = client.pods().inNamespace(namespace).withName(podName).getLog();
                    if (logs == null || logs.isBlank()) {
                        continue;
                    }
                    logs.lines()
                            .filter(line -> !line.isBlank())
                            .map(line -> toEvent(source, config, pod, line))
                            .forEach(onEvent);
                }
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

    private LogEventDto toEvent(LogSource source, Map<String, String> config, Pod pod, String line) {
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
                .timestamp(Instant.now())
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
