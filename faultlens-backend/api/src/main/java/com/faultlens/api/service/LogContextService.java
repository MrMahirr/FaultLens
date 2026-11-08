package com.faultlens.api.service;

import com.faultlens.api.dto.LogContextDto;
import com.faultlens.api.mapper.ApiDtoMapper;
import com.faultlens.common.dto.LogEntryDto;
import com.faultlens.common.exception.ResourceNotFoundException;
import com.faultlens.processor.model.LogEntry;
import com.faultlens.processor.repository.LogEntryRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Log context (P10 / 2.3): what the same source and pod logged just before and after an entry.
 *
 * <p>Two queries walk {@code idx_log_entries_context} (source_id, pod_name, timestamp, id) from the
 * anchor, one down and one up, each LIMIT n + 1 (the extra row says whether there is more). The
 * window is bounded to ±{@value #WINDOW_MINUTES} minutes so the planner touches only the
 * partitions (days) around the anchor. Same timestamp: ordered by id, compared as a row
 * ({@code (timestamp, id) < (?, ?)}), so paging from the edge entry neither skips nor repeats.
 * The row comparison does not prune partitions, so each side also bounds {@code timestamp}
 * plainly on both ends ({@code BETWEEN}): without it the "older" walk probed every partition
 * after the anchor (measured, bench/context.sql: all 32 partitions instead of 1–2).
 * A null source or pod matches null ({@code IS NULL}, which the index serves; {@code IS NOT
 * DISTINCT FROM} would not).
 */
@Service
public class LogContextService {
    static final int WINDOW_MINUTES = 60;
    public static final int MAX_LINES = 200;

    private final LogEntryRepository entries;
    private final EntityManager entityManager;
    private final ApiDtoMapper mapper;

    public LogContextService(LogEntryRepository entries, EntityManager entityManager, ApiDtoMapper mapper) {
        this.entries = entries;
        this.entityManager = entityManager;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public LogContextDto context(Long id, int before, int after) {
        LogEntry anchor = entries.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Log not found: " + id, "LOG_NOT_FOUND"));
        Duration window = Duration.ofMinutes(WINDOW_MINUTES);

        List<LogEntry> older = before == 0 ? List.of() : side(anchor, true, before + 1, window);
        List<LogEntry> newer = after == 0 ? List.of() : side(anchor, false, after + 1, window);

        List<LogEntryDto> result = new ArrayList<>(Math.min(older.size(), before) + 1 + Math.min(newer.size(), after));
        List<LogEntry> olderShown = new ArrayList<>(older.subList(0, Math.min(older.size(), before)));
        Collections.reverse(olderShown); // fetched newest first
        olderShown.forEach(entry -> result.add(mapper.toLogEntryDto(entry)));
        result.add(mapper.toLogEntryDto(anchor));
        newer.subList(0, Math.min(newer.size(), after)).forEach(entry -> result.add(mapper.toLogEntryDto(entry)));

        return new LogContextDto(anchor.getId(), result, older.size() > before, newer.size() > after, WINDOW_MINUTES);
    }

    @SuppressWarnings("unchecked")
    private List<LogEntry> side(LogEntry anchor, boolean older, int limit, Duration window) {
        StringBuilder sql = new StringBuilder("SELECT * FROM log_entries WHERE ");
        sql.append(anchor.getSourceId() == null ? "source_id IS NULL" : "source_id = :source");
        sql.append(anchor.getPodName() == null ? " AND pod_name IS NULL" : " AND pod_name = :pod");
        if (older) {
            sql.append(" AND (timestamp, id) < (:ts, :id) AND timestamp BETWEEN :bound AND :ts"
                    + " ORDER BY timestamp DESC, id DESC");
        } else {
            sql.append(" AND (timestamp, id) > (:ts, :id) AND timestamp BETWEEN :ts AND :bound"
                    + " ORDER BY timestamp, id");
        }
        sql.append(" LIMIT :limit");

        Query query = entityManager.createNativeQuery(sql.toString(), LogEntry.class);
        if (anchor.getSourceId() != null) {
            query.setParameter("source", anchor.getSourceId());
        }
        if (anchor.getPodName() != null) {
            query.setParameter("pod", anchor.getPodName());
        }
        Instant ts = anchor.getTimestamp();
        query.setParameter("ts", ts);
        query.setParameter("id", anchor.getId());
        query.setParameter("bound", older ? ts.minus(window) : ts.plus(window));
        query.setParameter("limit", limit);
        return query.getResultList();
    }
}
