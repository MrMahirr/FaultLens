package com.faultlens.collector.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.faultlens.collector.model.LogSource;
import com.faultlens.common.dto.LogEventDto;
import com.faultlens.common.enums.LogSourceType;
import com.faultlens.common.enums.Severity;
import com.faultlens.common.exception.CollectorException;
import java.io.File;
import java.io.RandomAccessFile;
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
public class LocalFileAdapter implements LogSourceAdapter {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Set<String> running = ConcurrentHashMap.newKeySet();

    @Override
    public LogSourceType getType() {
        return LogSourceType.LOCAL_FILE;
    }

    @Override
    public void startStreaming(LogSource source, Consumer<LogEventDto> onEvent) {
        String sourceId = String.valueOf(source.getId());
        running.add(sourceId);
        
        Map<String, String> config = parseConfig(source.getConfig());
        String path = config.get("logFilePath");
        if (path == null || path.isBlank()) {
            log.error("Local file path is missing for source {}", sourceId);
            running.remove(sourceId);
            return;
        }

        File file = new File(path);
        long filePointer = 0;

        // Start at the end of the file if it exists, to avoid reading history on startup
        if (file.exists()) {
            filePointer = file.length();
        }

        while (running.contains(sourceId) && !Thread.currentThread().isInterrupted()) {
            try {
                if (file.exists()) {
                    long len = file.length();
                    // If file is truncated or rotated, reset pointer
                    if (len < filePointer) {
                        filePointer = 0;
                    }
                    if (len > filePointer) {
                        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
                            raf.seek(filePointer);
                            String line;
                            while (running.contains(sourceId) && (line = raf.readLine()) != null) {
                                // RandomAccessFile readLine returns ISO-8859-1, convert to UTF-8
                                String utf8Line = new String(line.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
                                onEvent.accept(toEvent(source, utf8Line));
                            }
                            filePointer = raf.getFilePointer();
                        }
                    }
                }
                Thread.sleep(1000);
            } catch (Exception ex) {
                log.warn("LocalFileAdapter streaming error for source {}", sourceId, ex);
                sleep(5);
            }
        }
    }

    @Override
    public void stopStreaming(String sourceId) {
        running.remove(sourceId);
    }

    @Override
    public boolean testConnection(LogSource source) {
        Map<String, String> config = parseConfig(source.getConfig());
        String path = config.get("logFilePath");
        if (path == null || path.isBlank()) {
            return false;
        }
        File file = new File(path);
        // It's valid if the parent directory exists or if we can read the file
        return file.exists() || (file.getParentFile() != null && file.getParentFile().exists());
    }

    private LogEventDto toEvent(LogSource source, String line) {
        return LogEventDto.builder()
                .id(UUID.randomUUID().toString())
                .source("local_file")
                .cluster("localhost")
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
            throw new CollectorException("Invalid LocalFile source config", "INVALID_SOURCE_CONFIG", ex);
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
