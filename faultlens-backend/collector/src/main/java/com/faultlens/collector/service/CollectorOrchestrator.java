package com.faultlens.collector.service;

import com.faultlens.collector.adapter.LogSourceAdapter;
import com.faultlens.collector.adapter.SourceSink;
import com.faultlens.collector.kafka.LogEventProducer;
import com.faultlens.collector.model.LogSource;
import com.faultlens.collector.repository.LogSourceRepository;
import com.faultlens.common.dto.LogEventDto;
import jakarta.annotation.PreDestroy;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Runs the source adapters of this instance.
 *
 * <p>P08 — several instances: a source is collected only by the instance holding its
 * {@link SourceLeaseManager lease}. {@link #reconcile()} claims enabled sources nobody collects
 * (also those of a crashed instance, once its lease expired) and stops local ones that were
 * disabled or deleted elsewhere; {@link #heartbeat()} renews our leases and stops any source
 * whose lease another instance took over (it was edited there).
 */
@Slf4j
@Service
public class CollectorOrchestrator {
    private final LogSourceRepository repository;
    private final LogEventProducer producer;
    private final SourceLeaseManager leases;
    private final SourceHealthReporter health;
    private final Map<com.faultlens.common.enums.LogSourceType, LogSourceAdapter> adapters;
    private final Map<Long, Future<?>> runningTasks = new ConcurrentHashMap<>();
    /** Sources stopped on purpose; their task ending is not an error. */
    private final Set<Long> stopping = ConcurrentHashMap.newKeySet();
    /** Latest delivery time per source not yet written; one UPDATE per source per flush. */
    private final Map<Long, Instant> pendingLastSeen = new ConcurrentHashMap<>();
    private final Map<Long, MultilineAggregator> aggregators = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable);
        thread.setName("faultlens-collector-" + thread.threadId());
        thread.setDaemon(true);
        return thread;
    });

    public CollectorOrchestrator(LogSourceRepository repository, LogEventProducer producer, List<LogSourceAdapter> adapters,
                                 SourceLeaseManager leases, SourceHealthReporter health) {
        this.repository = repository;
        this.producer = producer;
        this.leases = leases;
        this.health = health;
        this.adapters = new EnumMap<>(com.faultlens.common.enums.LogSourceType.class);
        adapters.forEach(adapter -> this.adapters.put(adapter.getType(), adapter));
    }

    /**
     * Records logs pushed to an HTTP source (P10 / 1.2): marks it connected and updates its
     * last-seen time (written by {@link #flushLastSeen()}, like collected sources).
     */
    public void recordPushed(Long sourceId) {
        if (sourceId == null) {
            return;
        }
        health.connected(sourceId);
        pendingLastSeen.merge(sourceId, Instant.now(), (a, b) -> a.isAfter(b) ? a : b);
    }

    private static boolean isPushed(LogSource source) {
        return source.getType() != null && source.getType().isPushed();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        reconcile();
    }

    /**
     * Claims enabled sources that nobody collects, and stops local sources that are no longer
     * enabled (changed by another instance).
     */
    @Scheduled(initialDelayString = "${faultlens.collector.lease.reconcile-ms:15000}",
            fixedDelayString = "${faultlens.collector.lease.reconcile-ms:15000}")
    public void reconcile() {
        List<LogSource> enabled;
        try {
            enabled = repository.findByEnabledTrue();
        } catch (RuntimeException ex) {
            log.warn("Collector reconcile skipped: could not load sources", ex);
            return;
        }
        Set<Long> enabledIds = enabled.stream().map(LogSource::getId).collect(Collectors.toSet());
        for (Long running : List.copyOf(runningTasks.keySet())) {
            if (!enabledIds.contains(running)) {
                stopLocal(running);
                leases.release(running);
            }
        }
        for (LogSource source : enabled) {
            if (isPushed(source)) {
                continue; // HTTP sources push their logs; nothing to collect or lease
            }
            if (!isStreaming(source.getId()) && leases.tryAcquire(source.getId())) {
                startLocal(source);
            }
        }
    }

    /** Renews our leases; a source whose lease we lost is now collected elsewhere. */
    @Scheduled(fixedDelayString = "${faultlens.collector.lease.heartbeat-ms:10000}")
    public void heartbeat() {
        for (Long sourceId : List.copyOf(runningTasks.keySet())) {
            if (!leases.renew(sourceId)) {
                log.info("Lease of source {} moved to another instance; stopping it here", sourceId);
                stopLocal(sourceId);
                health.forget(sourceId);
            }
        }
    }

    /**
     * (Re)starts a source after it was created, enabled or edited on this instance: takes its
     * lease from whoever holds it, so the new configuration is used from now on.
     */
    public void startSource(LogSource source) {
        if (source.getId() == null || !source.isEnabled() || isPushed(source)) {
            return;
        }
        leases.takeOver(source.getId());
        startLocal(source);
    }

    /** Stops a source everywhere (it was disabled or deleted). */
    public void stopSource(Long sourceId) {
        if (sourceId == null) {
            return;
        }
        stopLocal(sourceId);
        leases.revoke(sourceId);
        health.idle(sourceId);
    }

    private void startLocal(LogSource source) {
        Future<?> existing = runningTasks.get(source.getId());
        if (existing != null && !existing.isDone() && !existing.isCancelled()) {
            return;
        }
        runningTasks.remove(source.getId());
        LogSourceAdapter adapter = adapters.get(source.getType());
        if (adapter == null) {
            log.warn("No adapter registered for source type {}", source.getType());
            health.error(source.getId(), "Bu kaynak tipi için adaptör yok: " + source.getType());
            return;
        }
        Long sourceId = source.getId();
        // Adapters deliver one event per line; the aggregator joins multi-line events (stack traces).
        MultilineAggregator aggregator = new MultilineAggregator(event -> {
            Instant now = Instant.now();
            // In memory for the adapter (resume point on retry); persisted by flushLastSeen().
            source.setLastSeenAt(now);
            pendingLastSeen.merge(sourceId, now, (a, b) -> a.isAfter(b) ? a : b);
            producer.send(event);
        });
        SourceSink sink = new SourceSink() {
            @Override
            public void accept(LogEventDto event) {
                health.connected(sourceId);
                aggregator.accept(event);
            }

            @Override
            public void error(String message) {
                if (!stopping.contains(sourceId)) {
                    health.error(sourceId, message);
                }
            }

            @Override
            public void error(Throwable error) {
                // A stop interrupts the adapter: that is not a failure of the source.
                if (!stopping.contains(sourceId) && !(error instanceof InterruptedException)) {
                    health.error(sourceId, error);
                }
            }
        };
        aggregators.put(sourceId, aggregator);
        stopping.remove(sourceId);
        health.connected(sourceId);
        Future<?> future = executorService.submit(() -> {
            try {
                adapter.startStreaming(source, sink);
            } catch (RuntimeException ex) {
                if (!stopping.contains(sourceId)) {
                    log.warn("Collector for source {} failed", sourceId, ex);
                    health.error(sourceId, ex);
                }
            } finally {
                aggregator.flush();
                aggregators.remove(sourceId, aggregator);
            }
        });
        runningTasks.put(sourceId, future);
    }

    private void stopLocal(Long sourceId) {
        stopping.add(sourceId);
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
     * True while the source's adapter is running on <b>this</b> instance. An enabled source whose
     * adapter returned (e.g. a LOCAL_FILE path outside the allow-list) or failed is not streaming.
     */
    public boolean isStreaming(Long sourceId) {
        Future<?> future = sourceId == null ? null : runningTasks.get(sourceId);
        return future != null && !future.isDone();
    }

    /** True if some instance holds the source's lease (is collecting it). */
    public boolean isCollectedAnywhere(Long sourceId) {
        return sourceId != null && (isStreaming(sourceId) || leases.ownerOf(sourceId).isPresent());
    }

    /**
     * Tests a source through its adapter.
     */
    public boolean testSource(LogSource source) {
        if (isPushed(source)) {
            return true; // nothing to connect to; the application calls us
        }
        LogSourceAdapter adapter = adapters.get(source.getType());
        return adapter != null && adapter.testConnection(source);
    }

    /**
     * Sends multi-line events whose source has been quiet for a moment (the last line of a stack
     * trace has no "next line" that would close it).
     */
    @Scheduled(fixedDelay = 250)
    public void flushIdleEvents() {
        aggregators.values().forEach(MultilineAggregator::flushIfIdle);
    }

    /**
     * Persists the pending last-seen times. Runs every 10 s instead of one write per log line
     * (a busy source produced one UPDATE per event).
     */
    @Scheduled(fixedDelayString = "${faultlens.collector.last-seen-flush-ms:10000}")
    public void flushLastSeen() {
        for (Long sourceId : List.copyOf(pendingLastSeen.keySet())) {
            Instant seenAt = pendingLastSeen.remove(sourceId);
            if (seenAt == null) {
                continue;
            }
            try {
                repository.touchLastSeenAt(sourceId, seenAt);
            } catch (RuntimeException ex) {
                // keep it for the next flush unless something newer arrived meanwhile
                pendingLastSeen.merge(sourceId, seenAt, (a, b) -> a.isAfter(b) ? a : b);
                log.warn("Could not persist lastSeenAt for source {}", sourceId, ex);
            }
        }
    }

    /**
     * Stops all running adapters and hands their sources back (another instance takes them over
     * at its next reconcile instead of waiting for the leases to expire).
     */
    @PreDestroy
    public void shutdown() {
        List<Long> sourceIds = List.copyOf(runningTasks.keySet());
        for (Long sourceId : sourceIds) {
            stopLocal(sourceId);
            leases.release(sourceId);
            health.idle(sourceId);
        }
        executorService.shutdownNow();
        flushLastSeen();
    }
}
