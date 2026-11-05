package com.faultlens.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;

/** SQL wiring and batching; what the statements really delete is checked in LogRetentionIT. */
class LogRetentionJobTest {
    private static final Instant NOW = Instant.parse("2026-11-05T03:00:00Z");

    private final SystemSettingsService settings = mock(SystemSettingsService.class);
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final LogRetentionJob job = new LogRetentionJob(settings, jdbc, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void zeroOrNegativeDaysDisablesCleanup() {
        when(settings.getLogRetentionDays()).thenReturn(0, -5);

        job.run();
        job.run();

        verifyNoInteractions(jdbc);
    }

    @Test
    void deletesEveryTableWithAParameterisedCutoffInOrder() {
        when(settings.getLogRetentionDays()).thenReturn(30);
        Timestamp cutoff = Timestamp.from(Instant.parse("2026-10-06T03:00:00Z"));

        job.run();

        InOrder order = inOrder(jdbc);
        order.verify(jdbc).update(LogRetentionJob.DELETE_ENTRIES, cutoff);
        order.verify(jdbc).update(LogRetentionJob.DELETE_ANALYSES, cutoff);
        order.verify(jdbc).update(LogRetentionJob.DELETE_RESOLVED_ALARMS, cutoff);
        order.verify(jdbc).update(LogRetentionJob.DELETE_EMPTY_GROUPS, cutoff);
    }

    @Test
    void keepsDeletingWhileFullBatchesComeBack() {
        when(settings.getLogRetentionDays()).thenReturn(7);
        when(jdbc.update(eq(LogRetentionJob.DELETE_ENTRIES), any(Timestamp.class)))
                .thenReturn(LogRetentionJob.BATCH_SIZE, LogRetentionJob.BATCH_SIZE, 12);

        LogRetentionJob.Result result = job.run();

        verify(jdbc, times(3)).update(eq(LogRetentionJob.DELETE_ENTRIES), any(Timestamp.class));
        verify(jdbc, times(1)).update(eq(LogRetentionJob.DELETE_ANALYSES), any(Timestamp.class));
        assertThat(result.logEntries()).isEqualTo(2L * LogRetentionJob.BATCH_SIZE + 12);
    }

    @Test
    void onlyResolvedAlarmsAreRemoved() {
        assertThat(LogRetentionJob.DELETE_RESOLVED_ALARMS).contains("status = 'RESOLVED'");
        assertThat(LogRetentionJob.DELETE_EMPTY_GROUPS).contains("NOT EXISTS");
    }

    @Test
    void cronRunsNightlyAtThree() throws Exception {
        Scheduled scheduled = LogRetentionJob.class.getMethod("cleanupOldLogs").getAnnotation(Scheduled.class);

        assertThat(scheduled.cron()).isEqualTo("0 0 3 * * *");
    }
}
