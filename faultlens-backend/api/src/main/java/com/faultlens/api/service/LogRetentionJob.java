package com.faultlens.api.service;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Nightly retention: removes data older than the configured number of days
 * ({@code system_settings.log_retention_days}; 0 = keep forever).
 *
 * <ul>
 *   <li>log entries older than the cutoff (their analyses go with them: ON DELETE CASCADE)</li>
 *   <li>analyses older than the cutoff (also those of a whole group, which have no entry)</li>
 *   <li>alarms that are RESOLVED and older than the cutoff; open alarms are kept however old</li>
 *   <li>log groups left without entries whose last occurrence is older than the cutoff</li>
 * </ul>
 * Every delete runs in batches of {@value #BATCH_SIZE} rows, each its own short statement, so a
 * large backlog does not become one huge transaction holding locks on the hot tables.
 */
@Slf4j
@Service
public class LogRetentionJob {
    static final int BATCH_SIZE = 5000;

    static final String DELETE_ENTRIES =
            "DELETE FROM log_entries WHERE id IN (SELECT id FROM log_entries WHERE timestamp < ? LIMIT " + BATCH_SIZE + ")";
    static final String DELETE_ANALYSES =
            "DELETE FROM analyses WHERE id IN (SELECT id FROM analyses WHERE analyzed_at < ? LIMIT " + BATCH_SIZE + ")";
    static final String DELETE_RESOLVED_ALARMS =
            "DELETE FROM alarms WHERE id IN (SELECT id FROM alarms WHERE status = 'RESOLVED' AND triggered_at < ? LIMIT "
                    + BATCH_SIZE + ")";
    static final String DELETE_EMPTY_GROUPS =
            "DELETE FROM log_groups WHERE id IN (SELECT g.id FROM log_groups g WHERE g.last_seen_at < ? "
                    + "AND NOT EXISTS (SELECT 1 FROM log_entries e WHERE e.group_id = g.id) LIMIT " + BATCH_SIZE + ")";

    private final SystemSettingsService systemSettingsService;
    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    @Autowired
    public LogRetentionJob(SystemSettingsService systemSettingsService, JdbcTemplate jdbcTemplate) {
        this(systemSettingsService, jdbcTemplate, Clock.systemUTC());
    }

    LogRetentionJob(SystemSettingsService systemSettingsService, JdbcTemplate jdbcTemplate, Clock clock) {
        this.systemSettingsService = systemSettingsService;
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
    }

    /** Result of one run, per table. */
    record Result(long logEntries, long analyses, long alarms, long logGroups) {
        static final Result NOTHING = new Result(0, 0, 0, 0);
    }

    /**
     * Runs every night at 03:00.
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupOldLogs() {
        run();
    }

    Result run() {
        int retentionDays = systemSettingsService.getLogRetentionDays();
        if (retentionDays <= 0) {
            log.info("Log retention is disabled (retention days = 0). Skipping cleanup.");
            return Result.NOTHING;
        }
        Timestamp cutoff = Timestamp.from(Instant.now(clock).minus(Duration.ofDays(retentionDays)));
        log.info("Starting retention cleanup: removing data older than {} days ({})", retentionDays, cutoff.toInstant());

        // Order matters: entries first (frees their groups), groups last.
        Result result = new Result(
                deleteInBatches(DELETE_ENTRIES, cutoff),
                deleteInBatches(DELETE_ANALYSES, cutoff),
                deleteInBatches(DELETE_RESOLVED_ALARMS, cutoff),
                deleteInBatches(DELETE_EMPTY_GROUPS, cutoff));
        log.info("Retention cleanup done: {} log entries, {} analyses, {} resolved alarms, {} empty groups removed",
                result.logEntries(), result.analyses(), result.alarms(), result.logGroups());
        return result;
    }

    private long deleteInBatches(String sql, Timestamp cutoff) {
        long total = 0;
        int deleted;
        do {
            deleted = jdbcTemplate.update(sql, cutoff);
            total += deleted;
        } while (deleted == BATCH_SIZE);
        return total;
    }
}
