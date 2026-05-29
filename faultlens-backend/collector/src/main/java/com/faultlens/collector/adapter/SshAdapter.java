package com.faultlens.collector.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.faultlens.collector.config.CollectorProperties;
import com.faultlens.collector.model.LogSource;
import com.faultlens.common.dto.LogEventDto;
import com.faultlens.common.enums.LogSourceType;
import com.faultlens.common.enums.Severity;
import com.faultlens.common.exception.CollectorException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SshAdapter implements LogSourceAdapter {
    private final CollectorProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Set<String> running = ConcurrentHashMap.newKeySet();
    private final Map<String, Session> activeSessions = new ConcurrentHashMap<>();
    private final Map<String, ChannelExec> activeChannels = new ConcurrentHashMap<>();

    /**
     * Returns SSH source type.
     */
    @Override
    public LogSourceType getType() {
        return LogSourceType.SSH;
    }

    /**
     * Streams remote log file lines over SSH.
     */
    @Override
    public void startStreaming(LogSource source, Consumer<LogEventDto> onEvent) {
        String sourceId = String.valueOf(source.getId());
        running.add(sourceId);
        while (running.contains(sourceId) && !Thread.currentThread().isInterrupted()) {
            try {
                streamOnce(source, onEvent);
            } catch (Exception ex) {
                log.warn("SSH stream interrupted for source {}, retrying", sourceId, ex);
                sleep(properties.getSsh().getRetryIntervalSeconds());
            }
        }
    }

    /**
     * Stops SSH streaming.
     */
    @Override
    public void stopStreaming(String sourceId) {
        running.remove(sourceId);
        ChannelExec channel = activeChannels.remove(sourceId);
        if (channel != null) {
            channel.disconnect();
        }
        Session session = activeSessions.remove(sourceId);
        if (session != null) {
            session.disconnect();
        }
    }

    /**
     * Tests SSH connection.
     */
    @Override
    public boolean testConnection(LogSource source) {
        Map<String, String> config = parseConfig(source.getConfig());
        Session session = null;
        try {
            session = openSession(config);
            return session.isConnected();
        } catch (Exception ex) {
            log.warn("SSH connection test failed for source {}", source.getId(), ex);
            return false;
        } finally {
            if (session != null) {
                session.disconnect();
            }
        }
    }

    private void streamOnce(LogSource source, Consumer<LogEventDto> onEvent) throws Exception {
        Map<String, String> config = parseConfig(source.getConfig());
        Session session = openSession(config);
        String sourceId = String.valueOf(source.getId());
        activeSessions.put(sourceId, session);
        String path = config.getOrDefault("logFilePath", config.getOrDefault("path", "/var/log/syslog"));
        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        activeChannels.put(sourceId, channel);
        channel.setCommand("tail -F " + path);
        channel.setInputStream(null);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(channel.getInputStream(), StandardCharsets.UTF_8))) {
            channel.connect(properties.getSsh().getConnectTimeoutMs());
            String line;
            while (running.contains(String.valueOf(source.getId())) && (line = reader.readLine()) != null) {
                onEvent.accept(toEvent(source, config, line));
            }
        } finally {
            activeChannels.remove(sourceId);
            activeSessions.remove(sourceId);
            if (channel.isConnected()) {
                channel.disconnect();
            }
            if (session.isConnected()) {
                session.disconnect();
            }
        }
    }

    private Session openSession(Map<String, String> config) throws Exception {
        JSch jsch = new JSch();
        String privateKeyPath = config.getOrDefault("privateKeyPath", config.get("privateKey"));
        if (privateKeyPath != null && !privateKeyPath.isBlank()) {
            jsch.addIdentity(privateKeyPath);
        }
        String username = config.getOrDefault("username", config.get("user"));
        Session session = jsch.getSession(username, config.get("host"), Integer.parseInt(config.getOrDefault("port", "22")));
        if (config.containsKey("password")) {
            session.setPassword(config.get("password"));
        }
        session.setConfig("StrictHostKeyChecking", "no");
        session.setServerAliveInterval(15000); // Send keepalive every 15 seconds
        session.setServerAliveCountMax(3);     // Drop after 3 missed keepalives
        session.connect(properties.getSsh().getConnectTimeoutMs());
        return session;
    }

    private LogEventDto toEvent(LogSource source, Map<String, String> config, String line) {
        return LogEventDto.builder()
                .id(UUID.randomUUID().toString())
                .source("ssh")
                .cluster(config.getOrDefault("host", source.getName()))
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
            throw new CollectorException("Invalid SSH source config", "INVALID_SOURCE_CONFIG", ex);
        }
    }

    private void sleep(long seconds) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
