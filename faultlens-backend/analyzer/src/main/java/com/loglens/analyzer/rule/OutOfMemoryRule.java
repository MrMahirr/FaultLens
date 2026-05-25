package com.loglens.analyzer.rule;

import com.loglens.analyzer.engine.AnalysisResult;
import com.loglens.common.dto.LogEntryDto;
import org.springframework.stereotype.Component;

@Component
public class OutOfMemoryRule implements AnalysisRule {
    /**
     * Matches JVM out of memory failures.
     */
    @Override
    public boolean matches(LogEntryDto entry) {
        String text = text(entry);
        return text.contains("OutOfMemoryError") || text.contains("OutOfMemory");
    }

    /**
     * Produces JVM memory guidance.
     */
    @Override
    public AnalysisResult analyze(LogEntryDto entry) {
        return new AnalysisResult(
                "JVM heap alani doldu.",
                "JVM -Xmx parametresini artir, heap dump al ve memory leak analizi yap.",
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
        return 20;
    }

    /**
     * Returns stable rule name.
     */
    @Override
    public String getRuleName() {
        return "OutOfMemoryRule";
    }

    private String text(LogEntryDto entry) {
        return (entry.getStackTrace() == null ? "" : entry.getStackTrace()) + " " + (entry.getMessage() == null ? "" : entry.getMessage());
    }
}
