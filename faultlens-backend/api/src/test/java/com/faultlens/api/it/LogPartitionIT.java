package com.faultlens.api.it;

import static org.assertj.core.api.Assertions.assertThat;

import com.faultlens.api.service.LogPartitionMaintenance;
import com.faultlens.api.service.SystemSettingsService;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** P08: daily partitions of log_entries (V19) and their retention. */
class LogPartitionIT extends AbstractIntegrationTest {
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private LogPartitionMaintenance partitions;
    @Autowired
    private SystemSettingsService settings;

    private static String partitionOf(LocalDate day) {
        return "log_entries_" + day.format(DateTimeFormatter.BASIC_ISO_DATE);
    }

    private String partitionHolding(String message) {
        return jdbc.queryForObject("select tableoid::regclass::text from log_entries where message = ?", String.class, message);
    }

    private void insert(String message, Instant at) {
        jdbc.update("insert into log_entries (severity, message, timestamp) values ('INFO', ?, ?)", message, Timestamp.from(at));
    }

    @Test
    void upcomingDaysArePreparedAndRowsLandInTheirDay() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        assertThat(partitions.dailyPartitions())
                .contains(partitionOf(today), partitionOf(today.plusDays(7)));
        assertThat(partitions.ensureUpcoming()).as("idempotent").isZero();

        String now = "part-now-" + UUID.randomUUID();
        insert(now, Instant.now());
        assertThat(partitionHolding(now)).isEqualTo(partitionOf(today));

        // Outside every daily partition: the default partition catches it.
        String ancient = "part-ancient-" + UUID.randomUUID();
        insert(ancient, Instant.parse("2001-01-01T00:00:00Z"));
        assertThat(partitionHolding(ancient)).isEqualTo("log_entries_default");
    }

    @Test
    void retentionDropsWholeExpiredDays() {
        int days = settings.getLogRetentionDays();
        LocalDate expiredDay = LocalDate.now(ZoneOffset.UTC).minusDays(days + 10L);
        jdbc.queryForObject("select faultlens_ensure_log_partitions(?, ?)", Integer.class,
                Date.valueOf(expiredDay), Date.valueOf(expiredDay));
        String message = "part-expired-" + UUID.randomUUID();
        insert(message, expiredDay.atTime(12, 0).toInstant(ZoneOffset.UTC));
        assertThat(partitionHolding(message)).isEqualTo(partitionOf(expiredDay));

        Instant cutoff = Instant.now().minus(java.time.Duration.ofDays(days));
        assertThat(partitions.dropOlderThan(cutoff)).isPositive();

        assertThat(partitions.dailyPartitions()).doesNotContain(partitionOf(expiredDay));
        assertThat(jdbc.queryForObject("select count(*) from log_entries where message = ?", Long.class, message)).isZero();
        // The current day is untouched.
        assertThat(partitions.dailyPartitions()).contains(partitionOf(LocalDate.now(ZoneOffset.UTC)));
    }
}
