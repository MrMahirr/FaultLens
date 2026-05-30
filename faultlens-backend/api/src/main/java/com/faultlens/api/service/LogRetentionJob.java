package com.faultlens.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogRetentionJob {

    private final SystemSettingsService systemSettingsService;
    private final JdbcTemplate jdbcTemplate;

    /**
     * Runs every night at 03:00 AM.
     * Deletes log entries older than the configured retention days.
     * If logRetentionDays is 0, retention is disabled (logs are kept forever).
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupOldLogs() {
        int retentionDays = systemSettingsService.getLogRetentionDays();
        
        if (retentionDays <= 0) {
            log.info("Log retention is disabled (retention days = 0). Skipping cleanup.");
            return;
        }

        log.info("Starting log retention cleanup. Deleting logs older than {} days...", retentionDays);
        
        String sql = "DELETE FROM log_entries WHERE timestamp < NOW() - INTERVAL '" + retentionDays + " days'";
        int deletedCount = jdbcTemplate.update(sql);
        
        log.info("Log retention cleanup completed. Deleted {} old log entries.", deletedCount);
    }
}
