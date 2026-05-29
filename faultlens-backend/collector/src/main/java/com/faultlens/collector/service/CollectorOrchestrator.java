package com.faultlens.collector.service;

import com.faultlens.collector.adapter.LogSourceAdapter;
import com.faultlens.collector.kafka.LogEventProducer;
import com.faultlens.collector.model.LogSource;
import com.faultlens.collector.repository.LogSourceRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CollectorOrchestrator {
    private final LogSourceRepository repository;
    private final LogEventProducer producer;
    private final Map<com.faultlens.common.enums.LogSourceType, LogSourceAdapter> adapters;
    private final Map<Long, Future<?>> runningTasks = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable);
        thread.setName("faultlens-collector-" + thread.threadId());
        thread.setDaemon(true);
        return thread;
    });

    public CollectorOrchestrator(LogSourceRepository repository, LogEventProducer producer, List<LogSourceAdapter> adapters) {
        this.repository = repository;
        this.producer = producer;
        this.adapters = new EnumMap<>(com.faultlens.common.enums.LogSourceType.class);
        adapters.forEach(adapter -> this.adapters.put(adapter.getType(), adapter));
    }

    /**
     * Starts all enabled sources after the Spring context is ready.
     */
    @PostConstruct
    public void startEnabledSources() {
        repository.findByEnabledTrue().forEach(this::startSource);
    }

    /**
     * Starts streaming for a source if it is not already running.
     */
    public void startSource(LogSource source) {
        if (source.getId() == null || !source.isEnabled()) {
            return;
        }
        Future<?> existing = runningTasks.get(source.getId());
        if (existing != null && !existing.isDone() && !existing.isCancelled()) {
            return;
        }
        runningTasks.remove(source.getId());
        LogSourceAdapter adapter = adapters.get(source.getType());
        if (adapter == null) {
            log.warn("No adapter registered for source type {}", source.getType());
            return;
        }
        Future<?> future = executorService.submit(() -> adapter.startStreaming(source, event -> {
            source.setLastSeenAt(Instant.now());
            repository.save(source);
            producer.send(event);
        }));
        runningTasks.put(source.getId(), future);
    }

    /**
     * Stops streaming for a source.
     */
    public void stopSource(Long sourceId) {
        if (sourceId == null) {
            return;
        }
        repository.findById(sourceId).ifPresent(source -> {
            LogSourceAdapter adapter = adapters.get(source.getType());
            if (adapter != null) {
                adapter.stopStreaming(String.valueOf(sourceId));
            }
        });
        Future<?> future = runningTasks.remove(sourceId);
        if (future != null) {
            future.cancel(true);
        }
    }

    /**
     * Tests a source through its adapter.
     */
    public boolean testSource(LogSource source) {
        LogSourceAdapter adapter = adapters.get(source.getType());
        return adapter != null && adapter.testConnection(source);
    }

    /**
     * Stops all running adapters.
     */
    @PreDestroy
    public void shutdown() {
        List<Long> sourceIds = List.copyOf(runningTasks.keySet());
        sourceIds.forEach(this::stopSource);
        executorService.shutdownNow();
    }
}
