package com.loglens.analyzer.rule;

import com.loglens.analyzer.engine.AnalysisResult;
import com.loglens.common.dto.LogEntryDto;
import org.springframework.stereotype.Component;

@Component
public class ClassNotFoundRule implements AnalysisRule {
    /**
     * Matches classpath failures.
     */
    @Override
    public boolean matches(LogEntryDto entry) {
        String text = text(entry);
        return text.contains("ClassNotFoundException") || text.contains("NoClassDefFoundError");
    }

    /**
     * Produces dependency guidance.
     */
    @Override
    public AnalysisResult analyze(LogEntryDto entry) {
        return new AnalysisResult(
                "Eksik bagimlilik veya classpath sorunu.",
                "pom.xml / build.gradle bagimliliklarini ve deployment artifact'ini kontrol et.",
                null,
                0.90,
                "RULE_BASED",
                getRuleName()
        );
    }

    /**
     * Returns rule priority.
     */
    @Override
    public int getPriority() {
        return 35;
    }

    /**
     * Returns stable rule name.
     */
    @Override
    public String getRuleName() {
        return "ClassNotFoundRule";
    }

    private String text(LogEntryDto entry) {
        return (entry.getStackTrace() == null ? "" : entry.getStackTrace()) + " " + (entry.getMessage() == null ? "" : entry.getMessage());
    }
}
