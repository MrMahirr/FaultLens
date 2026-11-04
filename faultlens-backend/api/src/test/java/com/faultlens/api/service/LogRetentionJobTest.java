package com.faultlens.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

/** SQL wiring only; that the statement really deletes the right rows is checked in LogRetentionIT. */
class LogRetentionJobTest {
    private final SystemSettingsService settings = mock(SystemSettingsService.class);
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final LogRetentionJob job = new LogRetentionJob(settings, jdbc);

    @Test
    void zeroDaysDisablesCleanup() {
        when(settings.getLogRetentionDays()).thenReturn(0);

        job.cleanupOldLogs();

        verifyNoInteractions(jdbc);
    }

    @Test
    void negativeDaysDisablesCleanup() {
        when(settings.getLogRetentionDays()).thenReturn(-5);

        job.cleanupOldLogs();

        verifyNoInteractions(jdbc);
    }

    @Test
    void deletesEntriesOlderThanTheConfiguredDays() {
        when(settings.getLogRetentionDays()).thenReturn(30);

        job.cleanupOldLogs();

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc).update(sql.capture());
        assertThat(sql.getValue())
                .startsWith("DELETE FROM log_entries")
                .contains("timestamp < NOW() - INTERVAL '30 days'");
    }

    @Test
    void cronRunsNightlyAtThree() throws Exception {
        org.springframework.scheduling.annotation.Scheduled scheduled = LogRetentionJob.class
                .getMethod("cleanupOldLogs")
                .getAnnotation(org.springframework.scheduling.annotation.Scheduled.class);

        assertThat(scheduled.cron()).isEqualTo("0 0 3 * * *");
        verifyNoInteractions(jdbc);
    }
}
