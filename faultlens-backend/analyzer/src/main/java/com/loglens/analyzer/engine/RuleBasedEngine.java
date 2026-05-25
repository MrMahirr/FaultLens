package com.loglens.analyzer.engine;

import com.loglens.analyzer.rule.AnalysisRule;
import com.loglens.common.dto.LogEntryDto;
import com.loglens.processor.model.LogGroup;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RuleBasedEngine implements AnalysisEngine {
    private final List<AnalysisRule> rules;

    public RuleBasedEngine(List<AnalysisRule> rules) {
        this.rules = rules.stream().sorted(Comparator.comparingInt(AnalysisRule::getPriority)).toList();
    }

    /**
     * Runs registered rules and returns the first matching analysis.
     */
    @Override
    public AnalysisResult analyze(LogEntryDto entry, LogGroup group) {
        return rules.stream()
                .filter(rule -> rule.matches(entry))
                .findFirst()
                .map(rule -> rule.analyze(entry))
                .orElseGet(AnalysisResult::unknown);
    }

    /**
     * Rule-based engine is always available.
     */
    @Override
    public boolean isAvailable() {
        return true;
    }
}
