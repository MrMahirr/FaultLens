package com.faultlens.analyzer.rule;

import com.faultlens.analyzer.engine.AnalysisResult;
import com.faultlens.common.dto.LogEntryDto;

public interface AnalysisRule {
    /**
     * Returns whether the rule matches the log entry.
     */
    boolean matches(LogEntryDto entry);

    /**
     * Produces analysis for a matching entry.
     */
    AnalysisResult analyze(LogEntryDto entry);

    /**
     * Returns rule priority; lower values run first.
     */
    int getPriority();

    /**
     * Returns a stable rule name.
     */
    String getRuleName();
}
