package com.faultlens.collector.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.faultlens.collector.model.LogSource;
import com.faultlens.common.dto.LogEventDto;
import com.faultlens.common.enums.LogSourceType;
import com.faultlens.common.enums.Severity;
import com.faultlens.common.exception.CollectorException;
import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DockerAdapter implements LogSourceAdapter {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Set<String> running = ConcurrentHashMap.newKeySet();
    private final Map<String, Closeable> activeStreams = new ConcurrentHashMap<>();

    /**
     * Returns Docker source type.
     */
    @Override
    public LogSourceType getType() {
        return LogSourceType.DOCKER;
    }

    /**
     * Streams Docker container log frames.
     */
    @Override
    public void startStreaming(LogSource source, Consumer<LogEventDto> onEvent) {
        String sourceId = String.valueOf(source.getId());
        running.add(sourceId);
        Map<String, String> config = parseConfig(source.getConfig());
        String containerId = config.get("containerId");
        try (DockerClient client = createClient(config)) {
            ResultCallback.Adapter<Frame> callback = new ResultCallback.Adapter<>() {
                @Override
                public void onNext(Frame frame) {
                    if (running.contains(sourceId)) {
                        String line = new String(frame.getPayload(), StandardCharsets.UTF_8).strip();
                        if (!line.isBlank()) {
                            onEvent.accept(toEvent(source, config, line));
                        }
                    }
                }
            };
            activeStreams.put(sourceId, callback);
            client.logContainerCmd(containerId)
                    .withFollowStream(true)
                    .withStdOut(true)
                    .withStdErr(true)
                    .exec(callback)
                    .awaitCompletion();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (Exception ex) {
            throw new CollectorException("Docker log streaming failed", "DOCKER_STREAM_FAILED", ex);
        } finally {
            closeActiveStream(sourceId);
            running.remove(sourceId);
        }
    }

    /**
     * Stops Docker streaming.
     */
    @Override
    public void stopStreaming(String sourceId) {
        running.remove(sourceId);
        closeActiveStream(sourceId);
    }

    /**
     * Tests Docker daemon and target container access.
     */
    @Override
    public boolean testConnection(LogSource source) {
        Map<String, String> config = parseConfig(source.getConfig());
        try (DockerClient client = createClient(config)) {
            client.inspectContainerCmd(config.get("containerId")).exec();
            return true;
        } catch (Exception ex) {
            log.warn("Docker connection test failed for source {}", source.getId(), ex);
            return false;
        }
    }

    private DockerClient createClient(Map<String, String> config) {
        String dockerHost = config.get("dockerHost");
        if (dockerHost == null || dockerHost.isBlank()) {
            return DockerClientBuilder.getInstance().build();
        }
        DockerClientConfig clientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(dockerHost)
                .build();
        return DockerClientBuilder.getInstance(clientConfig).build();
    }

    private LogEventDto toEvent(LogSource source, Map<String, String> config, String line) {
        return LogEventDto.builder()
                .id(UUID.randomUUID().toString())
                .source("docker")
                .cluster(config.getOrDefault("host", "local"))
                .containerName(config.get("containerId"))
                .serviceName(source.getName())
                .severity(Severity.INFO)
                .message(line)
                .rawLine(line)
                .timestamp(Instant.now())
                .labels(Map.of("sourceId", String.valueOf(source.getId())))
                .build();
    }

    private Map<String, String> parseConfig(String json) {
        try {
            if (json == null || json.isBlank()) {
                return Map.of();
            }
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception ex) {
            throw new CollectorException("Invalid Docker source config", "INVALID_SOURCE_CONFIG", ex);
        }
    }

    private void closeActiveStream(String sourceId) {
        Closeable closeable = activeStreams.remove(sourceId);
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException ex) {
            log.warn("Failed to close Docker log stream for source {}", sourceId, ex);
        }
    }
}
