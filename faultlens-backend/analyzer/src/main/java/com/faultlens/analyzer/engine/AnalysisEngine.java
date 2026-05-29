package com.faultlens.analyzer.engine;

import com.faultlens.common.dto.LogEntryDto;
import com.faultlens.processor.model.LogGroup;

public interface AnalysisEngine {
    /**
     * Analyzes a log entry and its group.
     */
    AnalysisResult analyze(LogEntryDto entry, LogGroup group);

    /**
     * Returns whether this engine is ready to process analysis requests.
     */
    boolean isAvailable();
}
