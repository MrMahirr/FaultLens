package com.faultlens.collector.service;

import com.faultlens.collector.repository.LogSourceRepository;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Records each source's collector health in {@code log_sources} (P08): CONNECTED while
 * collecting, ERROR with the last error message when the adapter fails, IDLE when nobody
 * collects it. Writes only on transitions (and for a changed error message), so a busy source
 * does not produce a write per line; adapters may report from any thread.
 */
@Slf4j
@Component
public class SourceHealthReporter {
    public static final String CONNECTED = "CONNECTED";
    public static final String ERROR = "ERROR";
    public static final String IDLE = "IDLE";
    static final int MAX_ERROR_LENGTH = 500;

    private record State(String status, String error) {
    }

    private final LogSourceRepository repository;
    private final SourceLeaseManager leases;
    private final Map<Long, State> last = new ConcurrentHashMap<>();

    public SourceHealthReporter(LogSourceRepository repository, SourceLeaseManager leases) {
        this.repository = repository;
        this.leases = leases;
    }

    public void connected(Long sourceId) {
        transition(sourceId, new State(CONNECTED, null));
    }

    public void idle(Long sourceId) {
        transition(sourceId, new State(IDLE, null));
    }

    public void error(Long sourceId, String message) {
        transition(sourceId, new State(ERROR, truncate(message)));
    }

    /** Short, single-line description of an exception for the Sources page. */
    public void error(Long sourceId, Throwable error) {
        Throwable root = error;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String message = root.getMessage() == null ? root.getClass().getSimpleName()
                : root.getClass().getSimpleName() + ": " + root.getMessage();
        error(sourceId, message);
    }

    /** Forgets the cached state (the source moved to another instance). */
    public void forget(Long sourceId) {
        if (sourceId != null) {
            last.remove(sourceId);
        }
    }

    private void transition(Long sourceId, State next) {
        if (sourceId == null || next.equals(last.put(sourceId, next))) {
            return;
        }
        try {
            if (ERROR.equals(next.status())) {
                repository.recordError(sourceId, next.error(), Instant.now(), leases.instanceId());
            } else {
                repository.updateStatus(sourceId, next.status(), leases.instanceId());
            }
        } catch (RuntimeException ex) {
            last.remove(sourceId); // retry on the next report
            log.warn("Could not record health {} of source {}", next.status(), sourceId, ex);
        }
    }

    private static String truncate(String message) {
        if (message == null) {
            return null;
        }
        String oneLine = message.replaceAll("\\s+", " ").strip();
        return oneLine.length() <= MAX_ERROR_LENGTH ? oneLine : oneLine.substring(0, MAX_ERROR_LENGTH - 1) + "…";
    }
}
