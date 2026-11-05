package com.faultlens.api.it;

import static org.assertj.core.api.Assertions.assertThat;

import com.faultlens.api.service.LogRetentionJob;
import com.faultlens.api.service.SystemSettingsService;
import com.faultlens.common.enums.Severity;
import com.faultlens.processor.model.LogEntry;
import com.faultlens.processor.repository.LogEntryRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Runs the retention job's real SQL against PostgreSQL. */
class LogRetentionIT extends AbstractIntegrationTest {
    @Autowired
    private LogRetentionJob job;
    @Autowired
    private SystemSettingsService settings;
    @Autowired
    private LogEntryRepository entries;

    @Test
    void deletesOnlyEntriesOlderThanTheRetentionWindow() {
        int days = settings.getLogRetentionDays();
        assertThat(days).as("default retention").isPositive();
        Instant now = Instant.now();

        LogEntry expired = entries.save(entry("retention-expired", now.minus(days + 1, ChronoUnit.DAYS)));
        LogEntry kept = entries.save(entry("retention-kept", now.minus(days - 1, ChronoUnit.DAYS)));

        job.cleanupOldLogs();

        assertThat(entries.findById(expired.getId())).isEmpty();
        assertThat(entries.findById(kept.getId())).isPresent();
    }

    private static LogEntry entry(String message, Instant timestamp) {
        return LogEntry.builder()
                .severity(Severity.INFO)
                .message(message)
                .serviceName("retention-it")
                .timestamp(timestamp)
                .build();
    }
}
