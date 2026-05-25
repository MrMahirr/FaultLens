package com.loglens.analyzer.rule;

import com.loglens.analyzer.engine.AnalysisResult;
import com.loglens.common.dto.LogEntryDto;
import org.springframework.stereotype.Component;

@Component
public class StackOverflowRule implements AnalysisRule {
    /**
     * Matches stack overflow errors.
     */
    @Override
    public boolean matches(LogEntryDto entry) {
        return text(entry).contains("StackOverflowError");
    }

    /**
     * Produces recursion guidance.
     */
    @Override
    public AnalysisResult analyze(LogEntryDto entry) {
        return new AnalysisResult(
                "Sonsuz recursive cagri.",
                "Stack trace'deki tekrar eden method cagrilarini incele, base case ekle.",
                null,
                0.95,
                "RULE_BASED",
                getRuleName()
        );
    }

    /**
     * Returns rule priority.
     */
    @Override
    public int getPriority() {
        return 15;
    }

    /**
     * Returns stable rule name.
     */
    @Override
    public String getRuleName() {
        return "StackOverflowRule";
    }

    private String text(LogEntryDto entry) {
        return (entry.getStackTrace() == null ? "" : entry.getStackTrace()) + " " + (entry.getMessage() == null ? "" : entry.getMessage());
    }
}
