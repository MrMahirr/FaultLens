package com.faultlens.api.alarm;

import com.faultlens.api.model.AlarmRule;
import com.faultlens.api.model.AlarmRuleType;
import com.faultlens.api.repository.AlarmRuleRepository;
import com.faultlens.api.service.AlarmDispatcherService;
import com.faultlens.common.dto.LogEntryDto;
import com.faultlens.processor.event.LogGroupCreatedEvent;
import com.faultlens.processor.event.LogProcessedEvent;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Evaluates the enabled alarm rules against processed log entries and new log groups (ADR 0001).
 * Runs after the processor's transaction committed, on the processor's thread. Rules are cached
 * and reloaded on change and every 30 s.
 */
@Slf4j
@Component
public class AlarmRuleEvaluator {
    private final AlarmRuleRepository repository;
    private final RuleWindowCounter windowCounter;
    private final AlarmDeduplicator deduplicator;
    private final AlarmDispatcherService dispatcher;

    private volatile List<CompiledRule> rules = List.of();

    public AlarmRuleEvaluator(AlarmRuleRepository repository, RuleWindowCounter windowCounter,
                              AlarmDeduplicator deduplicator, AlarmDispatcherService dispatcher) {
        this.repository = repository;
        this.windowCounter = windowCounter;
        this.deduplicator = deduplicator;
        this.dispatcher = dispatcher;
    }

    /** A rule with its pattern compiled once. */
    record CompiledRule(AlarmRule rule, Pattern pattern) {
    }

    /**
     * Reloads the enabled rules. Called on start, after every change and periodically.
     */
    @Scheduled(initialDelay = 0, fixedDelay = 30_000)
    public void reload() {
        try {
            rules = repository.findByEnabledTrue().stream()
                    .map(rule -> new CompiledRule(rule,
                            rule.getType() == AlarmRuleType.PATTERN && rule.getPattern() != null
                                    ? Pattern.compile(rule.getPattern()) : null))
                    .toList();
        } catch (RuntimeException ex) {
            log.warn("Could not reload alarm rules; keeping the previous {}", rules.size(), ex);
        }
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void onRulesChanged(AlarmRulesChangedEvent event) {
        reload();
    }

    /**
     * THRESHOLD and PATTERN rules over the processed entries of one batch.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onLogsProcessed(LogProcessedEvent event) {
        List<CompiledRule> current = rules;
        if (current.isEmpty() || event.toPublish() == null || event.toPublish().isEmpty()) {
            return;
        }
        for (CompiledRule compiled : current) {
            try {
                switch (compiled.rule().getType()) {
                    case THRESHOLD -> evaluateThreshold(compiled.rule(), event.toPublish());
                    case PATTERN -> evaluatePattern(compiled, event.toPublish());
                    case NEW_GROUP -> {
                        // see onGroupCreated
                    }
                }
            } catch (RuntimeException ex) {
                log.error("Alarm rule {} failed", compiled.rule().getId(), ex);
            }
        }
    }

    /**
     * NEW_GROUP rules: a log group was created.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onGroupCreated(LogGroupCreatedEvent event) {
        for (CompiledRule compiled : rules) {
            AlarmRule rule = compiled.rule();
            if (rule.getType() != AlarmRuleType.NEW_GROUP || !rule.appliesTo(event.sourceId())
                    || !rule.accepts(event.severity())) {
                continue;
            }
            if (deduplicator.tryAcquire("rule:" + rule.getId() + ":group:" + event.groupId(), cooldown(rule))) {
                dispatcher.raiseForRule(rule, event.severity(), event.sourceId(),
                        "Yeni hata grubu (Grup ID: %d)\n%s".formatted(event.groupId(), abbreviate(event.firstMessage())));
            }
        }
    }

    private void evaluateThreshold(AlarmRule rule, List<LogEntryDto> entries) {
        Map<Long, Long> matchesPerSource = new HashMap<>();
        long matches = 0;
        for (LogEntryDto entry : entries) {
            if (rule.appliesTo(entry.getSourceId()) && rule.accepts(entry.getSeverity())) {
                matches++;
                matchesPerSource.merge(entry.getSourceId() == null ? -1L : entry.getSourceId(), 1L, Long::sum);
            }
        }
        if (matches == 0) {
            return;
        }
        OptionalLong count = windowCounter.addAndCount(rule.getId(), rule.getWindowSec(), matches);
        if (count.isEmpty() || count.getAsLong() < rule.getThreshold()) {
            return;
        }
        if (deduplicator.tryAcquire("rule:" + rule.getId(), cooldown(rule))) {
            Long sourceId = matchesPerSource.size() == 1 ? matchesPerSource.keySet().iterator().next() : null;
            dispatcher.raiseForRule(rule, rule.getSeverity(), sourceId != null && sourceId < 0 ? null : sourceId,
                    "Eşik aşıldı: son %d sn içinde %d kayıt (eşik %d, seviye ≥ %s)".formatted(
                            rule.getWindowSec(), count.getAsLong(), rule.getThreshold(), rule.getSeverity()));
        }
    }

    private void evaluatePattern(CompiledRule compiled, List<LogEntryDto> entries) {
        AlarmRule rule = compiled.rule();
        if (compiled.pattern() == null) {
            return;
        }
        for (LogEntryDto entry : entries) {
            if (!rule.appliesTo(entry.getSourceId()) || !rule.accepts(entry.getSeverity())) {
                continue;
            }
            String text = entry.getMessage() == null ? "" : entry.getMessage();
            if (entry.getStackTrace() != null) {
                text = text + "\n" + entry.getStackTrace();
            }
            if (!compiled.pattern().matcher(text).find()) {
                continue;
            }
            String scope = entry.getGroupId() == null ? "entry:" + entry.getId() : "group:" + entry.getGroupId();
            if (deduplicator.tryAcquire("rule:" + rule.getId() + ":" + scope, cooldown(rule))) {
                dispatcher.raiseForRule(rule, entry.getSeverity(), entry.getSourceId(),
                        "Desen eşleşti: /%s/ (Grup ID: %s)\n%s".formatted(rule.getPattern(), entry.getGroupId(),
                                abbreviate(entry.getMessage())));
            }
        }
    }

    private static Duration cooldown(AlarmRule rule) {
        return Duration.ofSeconds(rule.getCooldownSec() == null ? 0 : rule.getCooldownSec());
    }

    private static String abbreviate(String text) {
        if (text == null) {
            return "";
        }
        return text.length() <= 500 ? text : text.substring(0, 500) + "…";
    }
}
