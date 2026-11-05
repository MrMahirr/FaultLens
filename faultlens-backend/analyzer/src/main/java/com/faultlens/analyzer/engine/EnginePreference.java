package com.faultlens.analyzer.engine;

/**
 * Which engine the operator prefers (Settings → System → default analysis engine).
 *
 * <p>Implemented by the api module, which owns the system settings; the analyzer cannot depend on
 * it (the dependency points the other way). Without an implementation the AI engine is used
 * whenever it is available.
 */
public interface EnginePreference {
    /**
     * True if AI analysis should be used (when the AI engine is available).
     */
    boolean preferAi();
}
