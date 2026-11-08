package com.faultlens.api.alarm;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.faultlens.api.model.AlarmChannel;
import com.faultlens.api.model.AlarmRule;
import com.faultlens.api.model.AlarmRuleType;
import com.faultlens.api.repository.AlarmRuleRepository;
import com.faultlens.api.service.AlarmDispatcherService;
import com.faultlens.common.dto.LogEntryDto;
import com.faultlens.common.enums.Severity;
import com.faultlens.processor.event.LogGroupCreatedEvent;
import com.faultlens.processor.event.LogGroupRegressedEvent;
import com.faultlens.processor.event.LogProcessedEvent;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AlarmRuleEvaluatorTest {
    private AlarmRuleRepository repository;
    private RuleWindowCounter counter;
    private AlarmDeduplicator dedup;
    private AlarmDispatcherService dispatcher;
    private AlarmRuleEvaluator evaluator;
    private final Set<String> dedupKeys = new HashSet<>();
    private final AtomicLong windowCount = new AtomicLong();

    @BeforeEach
    void setUp() {
        repository = mock(AlarmRuleRepository.class);
        counter = mock(RuleWindowCounter.class);
        dedup = mock(AlarmDeduplicator.class);
        dispatcher = mock(AlarmDispatcherService.class);
        // Fake Redis semantics: counts accumulate, dedup keys are claimed once.
        when(counter.addAndCount(anyLong(), anyInt(), anyLong()))
                .thenAnswer(inv -> OptionalLong.of(windowCount.addAndGet(inv.getArgument(2))));
        when(dedup.tryAcquire(anyString(), any(Duration.class))).thenAnswer(inv -> dedupKeys.add(inv.getArgument(0)));
        evaluator = new AlarmRuleEvaluator(repository, counter, dedup, dispatcher);
    }

    @Test
    void thresholdFiresOnceWhenReachedAndNotAgainDuringCooldown() {
        rules(threshold(1L, Severity.ERROR, 60, 5));

        evaluator.onLogsProcessed(batch(entries(4, Severity.ERROR, 1L)));
        verify(dispatcher, never()).raiseForRule(any(), any(), any(), anyString());

        evaluator.onLogsProcessed(batch(entries(1, Severity.ERROR, 1L))); // 5th → fires
        evaluator.onLogsProcessed(batch(entries(20, Severity.ERROR, 1L))); // still above, cooldown

        verify(dispatcher, times(1)).raiseForRule(any(AlarmRule.class), eq(Severity.ERROR), eq(1L),
                contains("5 kayıt"));
    }

    @Test
    void thresholdCountsOnlyEntriesAtOrAboveTheSeverityAndOfTheRulesSources() {
        AlarmRule rule = threshold(1L, Severity.ERROR, 60, 3);
        rule.setSourceIds(List.of(7L));
        rules(rule);

        List<LogEntryDto> mixed = new ArrayList<>();
        mixed.addAll(entries(5, Severity.WARN, 7L));     // too low
        mixed.addAll(entries(5, Severity.ERROR, 8L));    // other source
        mixed.addAll(entries(2, Severity.CRITICAL, 7L)); // counts
        evaluator.onLogsProcessed(batch(mixed));

        verify(counter).addAndCount(1L, 60, 2L);
        verify(dispatcher, never()).raiseForRule(any(), any(), any(), anyString());
    }

    @Test
    void thresholdIsSkippedWhenTheCounterIsUnavailable() {
        rules(threshold(1L, Severity.ERROR, 60, 1));
        when(counter.addAndCount(anyLong(), anyInt(), anyLong())).thenReturn(OptionalLong.empty());

        evaluator.onLogsProcessed(batch(entries(10, Severity.ERROR, 1L)));

        verify(dispatcher, never()).raiseForRule(any(), any(), any(), anyString());
    }

    @Test
    void patternFiresOncePerGroupWithinTheCooldown() {
        rules(pattern(2L, Severity.INFO, "Connection (refused|reset)"));

        evaluator.onLogsProcessed(batch(List.of(
                entry(Severity.ERROR, 1L, 100L, "java.net.ConnectException: Connection refused"),
                entry(Severity.ERROR, 1L, 100L, "java.net.ConnectException: Connection refused"),
                entry(Severity.WARN, 1L, 200L, "upstream: Connection reset by peer"),
                entry(Severity.INFO, 1L, 300L, "connected fine"))));

        verify(dispatcher).raiseForRule(any(AlarmRule.class), eq(Severity.ERROR), eq(1L), contains("Grup ID: 100"));
        verify(dispatcher).raiseForRule(any(AlarmRule.class), eq(Severity.WARN), eq(1L), contains("Grup ID: 200"));
        verify(dispatcher, times(2)).raiseForRule(any(), any(), any(), anyString());
    }

    @Test
    void patternAlsoSearchesTheStackTrace() {
        rules(pattern(2L, Severity.ERROR, "OrderService\\.pay"));
        LogEntryDto e = entry(Severity.ERROR, 1L, 5L, "java.lang.IllegalStateException: boom");
        e.setStackTrace("\tat com.example.OrderService.pay(OrderService.java:42)");

        evaluator.onLogsProcessed(batch(List.of(e)));

        verify(dispatcher).raiseForRule(any(AlarmRule.class), eq(Severity.ERROR), eq(1L), anyString());
    }

    @Test
    void regressionRuleFiresOncePerRegression() {
        rules(rule(4L, AlarmRuleType.REGRESSION, Severity.ERROR));

        evaluator.onGroupRegressed(new LogGroupRegressedEvent(20L, Severity.ERROR, 1L, "boom", 1));
        evaluator.onGroupRegressed(new LogGroupRegressedEvent(20L, Severity.ERROR, 1L, "boom", 1)); // same regression
        evaluator.onGroupRegressed(new LogGroupRegressedEvent(20L, Severity.ERROR, 1L, "boom", 2)); // regressed again
        evaluator.onGroupRegressed(new LogGroupRegressedEvent(21L, Severity.WARN, 1L, "minor", 1)); // too low

        verify(dispatcher).raiseForRule(any(AlarmRule.class), eq(Severity.ERROR), eq(1L), contains("1. regresyon"));
        verify(dispatcher).raiseForRule(any(AlarmRule.class), eq(Severity.ERROR), eq(1L), contains("2. regresyon"));
        verify(dispatcher, times(2)).raiseForRule(any(), any(), any(), anyString());
    }

    @Test
    void newGroupRulesIgnoreRegressionsAndViceVersa() {
        rules(rule(3L, AlarmRuleType.NEW_GROUP, Severity.ERROR));
        evaluator.onGroupRegressed(new LogGroupRegressedEvent(20L, Severity.ERROR, 1L, "boom", 1));

        rules(rule(4L, AlarmRuleType.REGRESSION, Severity.ERROR));
        evaluator.onGroupCreated(new LogGroupCreatedEvent(11L, Severity.ERROR, 1L, "boom"));

        verify(dispatcher, never()).raiseForRule(any(), any(), any(), anyString());
    }

    @Test
    void entriesOfIgnoredGroupsRaiseNoAlarms() {
        rules(pattern(2L, Severity.ERROR, "boom"), threshold(1L, Severity.ERROR, 60, 2));

        evaluator.onLogsProcessed(new LogProcessedEvent(List.of(), List.of(
                entry(Severity.ERROR, 1L, 100L, "boom"),
                entry(Severity.ERROR, 1L, 100L, "boom"),
                entry(Severity.ERROR, 1L, 200L, "boom")), Set.of(100L)));

        // Only the entry of group 200 counts: the pattern fires for it, the threshold (2) is not reached.
        verify(counter).addAndCount(1L, 60, 1L);
        verify(dispatcher).raiseForRule(any(AlarmRule.class), eq(Severity.ERROR), eq(1L), contains("Grup ID: 200"));
        verify(dispatcher, times(1)).raiseForRule(any(), any(), any(), anyString());
    }

    @Test
    void newGroupRuleFiresForNewGroupsOfEnoughSeverity() {
        rules(rule(3L, AlarmRuleType.NEW_GROUP, Severity.ERROR));

        evaluator.onGroupCreated(new LogGroupCreatedEvent(10L, Severity.WARN, 1L, "a warning"));
        evaluator.onGroupCreated(new LogGroupCreatedEvent(11L, Severity.ERROR, 1L, "java.lang.NullPointerException"));
        evaluator.onGroupCreated(new LogGroupCreatedEvent(12L, Severity.CRITICAL, null, "OutOfMemoryError"));

        verify(dispatcher).raiseForRule(any(AlarmRule.class), eq(Severity.ERROR), eq(1L), contains("Grup ID: 11"));
        verify(dispatcher).raiseForRule(any(AlarmRule.class), eq(Severity.CRITICAL), isNull(), contains("Grup ID: 12"));
        verify(dispatcher, times(2)).raiseForRule(any(), any(), any(), anyString());
    }

    @Test
    void disabledRulesAreNotLoaded() {
        when(repository.findByEnabledTrue()).thenReturn(List.of());
        evaluator.reload();

        evaluator.onLogsProcessed(batch(entries(100, Severity.CRITICAL, 1L)));

        verify(dispatcher, never()).raiseForRule(any(), any(), any(), anyString());
    }

    @Test
    void oneFailingRuleDoesNotStopTheOthers() {
        AlarmRule broken = threshold(1L, Severity.ERROR, 60, 1);
        rules(broken, pattern(2L, Severity.ERROR, "boom"));
        when(counter.addAndCount(eq(1L), anyInt(), anyLong())).thenThrow(new IllegalStateException("bug"));

        evaluator.onLogsProcessed(batch(List.of(entry(Severity.ERROR, 1L, 9L, "boom"))));

        verify(dispatcher).raiseForRule(any(AlarmRule.class), eq(Severity.ERROR), eq(1L), contains("boom"));
    }

    private void rules(AlarmRule... rules) {
        when(repository.findByEnabledTrue()).thenReturn(List.of(rules));
        evaluator.reload();
    }

    private static AlarmRule rule(Long id, AlarmRuleType type, Severity severity) {
        return AlarmRule.builder().id(id).name("rule " + id).type(type).severity(severity)
                .channels(new ArrayList<>(List.of(AlarmChannel.IN_APP))).cooldownSec(900).enabled(true).build();
    }

    private static AlarmRule threshold(Long id, Severity severity, int windowSec, int threshold) {
        AlarmRule rule = rule(id, AlarmRuleType.THRESHOLD, severity);
        rule.setWindowSec(windowSec);
        rule.setThreshold(threshold);
        return rule;
    }

    private static AlarmRule pattern(Long id, Severity severity, String regex) {
        AlarmRule rule = rule(id, AlarmRuleType.PATTERN, severity);
        rule.setPattern(regex);
        return rule;
    }

    private static List<LogEntryDto> entries(int n, Severity severity, Long sourceId) {
        List<LogEntryDto> list = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            list.add(entry(severity, sourceId, 1L, "line " + i));
        }
        return list;
    }

    private static LogEntryDto entry(Severity severity, Long sourceId, Long groupId, String message) {
        return LogEntryDto.builder().id((long) message.hashCode()).severity(severity).sourceId(sourceId)
                .groupId(groupId).message(message).build();
    }

    private static LogProcessedEvent batch(List<LogEntryDto> entries) {
        return new LogProcessedEvent(List.of(), entries);
    }
}
