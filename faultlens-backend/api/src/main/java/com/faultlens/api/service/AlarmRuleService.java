package com.faultlens.api.service;

import com.faultlens.api.alarm.AlarmRulesChangedEvent;
import com.faultlens.api.dto.AlarmRuleDtos.AlarmRuleRequest;
import com.faultlens.api.dto.AlarmRuleDtos.AlarmRuleResponse;
import com.faultlens.api.model.AlarmChannel;
import com.faultlens.api.model.AlarmRule;
import com.faultlens.api.model.AlarmRuleType;
import com.faultlens.api.repository.AlarmRuleRepository;
import com.faultlens.common.exception.FaultLensException;
import com.faultlens.common.exception.ResourceNotFoundException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CRUD for alarm rules, with the type-specific validation bean validation cannot express.
 * Changes are picked up by {@link AlarmRuleEvaluator} on its next refresh (a few seconds).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlarmRuleService {
    private final AlarmRuleRepository repository;
    private final ApplicationEventPublisher events;

    public List<AlarmRuleResponse> list() {
        return repository.findAllByOrderByCreatedAtDesc().stream().map(AlarmRuleService::toResponse).toList();
    }

    public AlarmRuleResponse get(Long id) {
        return toResponse(find(id));
    }

    @Transactional
    public AlarmRuleResponse create(AlarmRuleRequest request, String username) {
        Instant now = Instant.now();
        AlarmRule rule = new AlarmRule();
        rule.setCreatedBy(username);
        rule.setCreatedAt(now);
        apply(rule, request);
        rule.setUpdatedAt(now);
        AlarmRuleResponse saved = toResponse(repository.save(rule));
        events.publishEvent(new AlarmRulesChangedEvent());
        return saved;
    }

    @Transactional
    public AlarmRuleResponse update(Long id, AlarmRuleRequest request) {
        AlarmRule rule = find(id);
        apply(rule, request);
        rule.setUpdatedAt(Instant.now());
        AlarmRuleResponse saved = toResponse(repository.save(rule));
        events.publishEvent(new AlarmRulesChangedEvent());
        return saved;
    }

    @Transactional
    public AlarmRuleResponse setEnabled(Long id, boolean enabled) {
        AlarmRule rule = find(id);
        rule.setEnabled(enabled);
        rule.setUpdatedAt(Instant.now());
        AlarmRuleResponse saved = toResponse(repository.save(rule));
        events.publishEvent(new AlarmRulesChangedEvent());
        return saved;
    }

    /**
     * Deletes the rule; alarms it raised keep their history (alarm_rule_id → NULL).
     */
    @Transactional
    public void delete(Long id) {
        repository.delete(find(id));
        events.publishEvent(new AlarmRulesChangedEvent());
    }

    private AlarmRule find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alarm rule not found: " + id, "ALARM_RULE_NOT_FOUND"));
    }

    private static void apply(AlarmRule rule, AlarmRuleRequest request) {
        AlarmRuleType type = request.type();
        rule.setName(request.name().trim());
        rule.setType(type);
        rule.setSeverity(request.severity());
        rule.setWindowSec(null);
        rule.setThreshold(null);
        rule.setPattern(null);
        switch (type) {
            case THRESHOLD -> {
                if (request.windowSec() == null || request.threshold() == null) {
                    throw new FaultLensException("THRESHOLD rules need window_sec and threshold", "INVALID_ALARM_RULE");
                }
                rule.setWindowSec(request.windowSec());
                rule.setThreshold(request.threshold());
            }
            case PATTERN -> {
                if (request.pattern() == null || request.pattern().isBlank()) {
                    throw new FaultLensException("PATTERN rules need a pattern", "INVALID_ALARM_RULE");
                }
                try {
                    Pattern.compile(request.pattern());
                } catch (PatternSyntaxException ex) {
                    throw new FaultLensException("Invalid pattern: " + ex.getDescription(), "INVALID_ALARM_RULE");
                }
                rule.setPattern(request.pattern());
            }
            case NEW_GROUP, REGRESSION -> {
                // severity and sources only
            }
        }
        rule.setSourceIds(request.sourceIds() == null ? new ArrayList<>()
                : new ArrayList<>(new LinkedHashSet<>(request.sourceIds())));
        List<AlarmChannel> channels = request.channels() == null || request.channels().isEmpty()
                ? List.of(AlarmChannel.IN_APP) : List.copyOf(new LinkedHashSet<>(request.channels()));
        rule.setChannels(new ArrayList<>(channels));
        rule.setCooldownSec(request.cooldownSec() == null ? 900 : request.cooldownSec());
        rule.setEnabled(request.enabled() == null || request.enabled());
    }

    static AlarmRuleResponse toResponse(AlarmRule rule) {
        return new AlarmRuleResponse(rule.getId(), rule.getName(), rule.getType(), rule.getSeverity(),
                rule.getWindowSec(), rule.getThreshold(), rule.getPattern(), List.copyOf(rule.getSourceIds()),
                List.copyOf(rule.getChannels()), rule.getCooldownSec(), rule.isEnabled(), rule.getCreatedBy(),
                rule.getCreatedAt(), rule.getUpdatedAt());
    }
}
