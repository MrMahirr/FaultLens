package com.faultlens.api.it;

import static org.assertj.core.api.Assertions.assertThat;

import com.faultlens.analyzer.model.Analysis;
import com.faultlens.analyzer.repository.AnalysisRepository;
import com.faultlens.api.model.Alarm;
import com.faultlens.api.model.AlarmStatus;
import com.faultlens.api.repository.AlarmRepository;
import com.faultlens.api.service.LogRetentionJob;
import com.faultlens.api.service.SystemSettingsService;
import com.faultlens.common.enums.Severity;
import com.faultlens.processor.model.LogEntry;
import com.faultlens.processor.model.LogGroup;
import com.faultlens.processor.repository.LogEntryRepository;
import com.faultlens.processor.repository.LogGroupRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
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
    @Autowired
    private LogGroupRepository groups;
    @Autowired
    private AnalysisRepository analyses;
    @Autowired
    private AlarmRepository alarms;

    @Test
    void removesExpiredDataAndKeepsRecentOrOpenData() {
        int days = settings.getLogRetentionDays();
        assertThat(days).as("default retention").isPositive();
        Instant old = Instant.now().minus(days + 1, ChronoUnit.DAYS);
        Instant recent = Instant.now().minus(days - 1, ChronoUnit.DAYS);

        // A group whose only entry expires → the group goes too.
        LogGroup expiredGroup = groups.save(group(old));
        LogEntry expired = entries.save(entry("retention-expired", old, expiredGroup.getId()));
        // A group that still has a recent entry stays, even though it was first seen long ago.
        LogGroup liveGroup = groups.save(group(old));
        LogEntry expiredInLiveGroup = entries.save(entry("retention-expired-2", old, liveGroup.getId()));
        LogEntry kept = entries.save(entry("retention-kept", recent, liveGroup.getId()));

        Analysis expiredAnalysis = analyses.save(analysis(liveGroup.getId(), null, old)); // group-level, no entry
        Analysis keptAnalysis = analyses.save(analysis(liveGroup.getId(), kept.getId(), recent));

        Alarm oldResolved = alarms.save(alarm(AlarmStatus.RESOLVED, old));
        Alarm oldActive = alarms.save(alarm(AlarmStatus.ACTIVE, old));
        Alarm oldAcknowledged = alarms.save(alarm(AlarmStatus.ACKNOWLEDGED, old));
        Alarm recentResolved = alarms.save(alarm(AlarmStatus.RESOLVED, recent));

        job.cleanupOldLogs();

        assertThat(entries.findById(expired.getId())).isEmpty();
        assertThat(entries.findById(expiredInLiveGroup.getId())).isEmpty();
        assertThat(entries.findById(kept.getId())).isPresent();

        assertThat(groups.findById(expiredGroup.getId())).as("left empty and old").isEmpty();
        assertThat(groups.findById(liveGroup.getId())).as("still has an entry").isPresent();

        assertThat(analyses.findById(expiredAnalysis.getId())).isEmpty();
        assertThat(analyses.findById(keptAnalysis.getId())).isPresent();

        assertThat(alarms.findById(oldResolved.getId())).isEmpty();
        assertThat(alarms.findById(oldActive.getId())).as("open alarms are kept").isPresent();
        assertThat(alarms.findById(oldAcknowledged.getId())).as("open alarms are kept").isPresent();
        assertThat(alarms.findById(recentResolved.getId())).isPresent();
    }

    private static LogGroup group(Instant seen) {
        return LogGroup.builder()
                .fingerprint("retention-it-" + UUID.randomUUID())
                .firstMessage("retention")
                .severity(Severity.ERROR)
                .count(1L)
                .firstSeenAt(seen)
                .lastSeenAt(seen)
                .build();
    }

    private static LogEntry entry(String message, Instant timestamp, Long groupId) {
        return LogEntry.builder()
                .severity(Severity.INFO)
                .message(message)
                .serviceName("retention-it")
                .groupId(groupId)
                .timestamp(timestamp)
                .build();
    }

    private static Analysis analysis(Long groupId, Long entryId, Instant at) {
        return Analysis.builder()
                .logGroupId(groupId)
                .logEntryId(entryId)
                .rootCause("retention")
                .engineType("RULE_BASED")
                .confidenceScore(0.5)
                .analyzedAt(at)
                .build();
    }

    private static Alarm alarm(AlarmStatus status, Instant triggeredAt) {
        return Alarm.builder()
                .ruleId("RETENTION_IT")
                .ruleName("retention")
                .severity(Severity.ERROR)
                .status(status)
                .message("retention")
                .triggeredAt(triggeredAt)
                .build();
    }
}
