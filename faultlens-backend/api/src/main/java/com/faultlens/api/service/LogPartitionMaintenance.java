package com.faultlens.api.service;

import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Daily partitions of {@code log_entries} (P08, V19).
 *
 * <ul>
 *   <li>at startup and every night: creates the partitions for today and the next
 *       {@value #DAYS_AHEAD} days, so new rows never land in the default partition;</li>
 *   <li>for retention: drops the partitions whose whole day is older than a cutoff (one cheap
 *       {@code DROP TABLE} instead of deleting the day's rows one by one).</li>
 * </ul>
 * Safe with several instances: a concurrent create is reported and ignored, and each DROP
 * waits at most {@value #LOCK_TIMEOUT} for its lock, otherwise it is retried the next night.
 */
@Slf4j
@Service
public class LogPartitionMaintenance {
    static final int DAYS_AHEAD = 7;
    static final String LOCK_TIMEOUT = "5s";
    static final Pattern DAILY = Pattern.compile("^log_entries_(\\d{8})$");
    static final String PARTITIONS = """
            SELECT c.relname FROM pg_inherits i
            JOIN pg_class c ON c.oid = i.inhrelid
            JOIN pg_class p ON p.oid = i.inhparent
            WHERE p.relname = 'log_entries'
            ORDER BY c.relname
            """;
    private static final DateTimeFormatter DAY = DateTimeFormatter.BASIC_ISO_DATE;

    private final JdbcTemplate jdbc;
    private final Clock clock;

    @Autowired
    public LogPartitionMaintenance(JdbcTemplate jdbc) {
        this(jdbc, Clock.systemUTC());
    }

    LogPartitionMaintenance(JdbcTemplate jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        ensureUpcoming();
    }

    /** Every night at 00:30 UTC, well before the next day starts. */
    @Scheduled(cron = "0 30 0 * * *", zone = "UTC")
    public void nightly() {
        ensureUpcoming();
    }

    /** Creates missing partitions for today .. today + {@value #DAYS_AHEAD}; returns how many. */
    public int ensureUpcoming() {
        LocalDate today = LocalDate.now(clock.withZone(ZoneOffset.UTC));
        try {
            Integer created = jdbc.queryForObject("SELECT faultlens_ensure_log_partitions(?, ?)", Integer.class,
                    java.sql.Date.valueOf(today), java.sql.Date.valueOf(today.plusDays(DAYS_AHEAD)));
            if (created != null && created > 0) {
                log.info("Created {} log_entries partition(s) up to {}", created, today.plusDays(DAYS_AHEAD));
            }
            return created == null ? 0 : created;
        } catch (DataAccessException ex) {
            // e.g. another instance created the same partition at the same moment
            log.warn("Could not create upcoming log partitions (will retry): {}", ex.getMessage());
            return 0;
        }
    }

    /** Names of the daily partitions, oldest first. */
    public List<String> dailyPartitions() {
        return jdbc.queryForList(PARTITIONS, String.class).stream()
                .filter(name -> DAILY.matcher(name).matches())
                .toList();
    }

    /** Drops every daily partition whose whole day ends at or before {@code cutoff}. */
    public int dropOlderThan(Instant cutoff) {
        int dropped = 0;
        for (String partition : dailyPartitions()) {
            Matcher matcher = DAILY.matcher(partition);
            if (!matcher.matches()) {
                continue;
            }
            Instant dayEnd = LocalDate.parse(matcher.group(1), DAY).plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            if (dayEnd.isAfter(cutoff)) {
                break; // sorted: the remaining days are newer
            }
            if (drop(partition)) {
                dropped++;
            }
        }
        return dropped;
    }

    private boolean drop(String partition) {
        try {
            jdbc.execute((java.sql.Connection connection) -> {
                try (Statement statement = connection.createStatement()) {
                    statement.execute("SET lock_timeout = '" + LOCK_TIMEOUT + "'");
                    try {
                        // name comes from pg_class and matched ^log_entries_\d{8}$
                        statement.execute("DROP TABLE IF EXISTS \"" + partition + "\"");
                    } finally {
                        statement.execute("RESET lock_timeout");
                    }
                }
                return null;
            });
            log.info("Retention: dropped partition {}", partition);
            return true;
        } catch (DataAccessException ex) {
            log.warn("Retention: could not drop partition {} now (will retry): {}", partition, ex.getMessage());
            return false;
        }
    }
}
