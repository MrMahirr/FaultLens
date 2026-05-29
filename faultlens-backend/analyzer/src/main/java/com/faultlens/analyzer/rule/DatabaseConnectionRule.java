package com.faultlens.analyzer.rule;

import com.faultlens.analyzer.engine.AnalysisResult;
import com.faultlens.common.dto.LogEntryDto;
import org.springframework.stereotype.Component;

@Component
public class DatabaseConnectionRule implements AnalysisRule {
    /**
     * Matches SQL and connection pool failures.
     */
    @Override
    public boolean matches(LogEntryDto entry) {
        String text = text(entry);
        return text.contains("SQLException")
                || text.contains("could not acquire connection")
                || text.contains("too many connections")
                || text.contains("Connection pool exhausted");
    }

    /**
     * Produces database pool guidance.
     */
    @Override
    public AnalysisResult analyze(LogEntryDto entry) {
        return new AnalysisResult(
                "Veritabani baglanti havuzu yetersiz veya DB yanit vermiyor.",
                "HikariCP pool boyutunu artir, uzun suren sorgulari ve connection leak'leri kontrol et.",
                null,
                0.88,
                "RULE_BASED",
                getRuleName()
        );
    }

    /**
     * Returns rule priority.
     */
    @Override
    public int getPriority() {
        return 25;
    }

    /**
     * Returns stable rule name.
     */
    @Override
    public String getRuleName() {
        return "DatabaseConnectionRule";
    }

    private String text(LogEntryDto entry) {
        return (entry.getStackTrace() == null ? "" : entry.getStackTrace()) + " " + (entry.getMessage() == null ? "" : entry.getMessage());
    }
}
