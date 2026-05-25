package com.loglens.analyzer.rule;

import com.loglens.analyzer.engine.AnalysisResult;
import com.loglens.common.dto.LogEntryDto;
import org.springframework.stereotype.Component;

@Component
public class ConnectionTimeoutRule implements AnalysisRule {
    /**
     * Matches network and socket timeout failures.
     */
    @Override
    public boolean matches(LogEntryDto entry) {
        String text = text(entry);
        return text.contains("Connection refused")
                || text.contains("Connection timed out")
                || text.contains("SocketTimeoutException")
                || text.contains("ConnectException");
    }

    /**
     * Produces dependency reachability guidance.
     */
    @Override
    public AnalysisResult analyze(LogEntryDto entry) {
        return new AnalysisResult(
                "Bagimli servis veya veritabanina ulasilamiyor.",
                "Network policy, servis health durumu ve firewall kurallarini kontrol et.",
                null,
                0.85,
                "RULE_BASED",
                getRuleName()
        );
    }

    /**
     * Returns rule priority.
     */
    @Override
    public int getPriority() {
        return 30;
    }

    /**
     * Returns stable rule name.
     */
    @Override
    public String getRuleName() {
        return "ConnectionTimeoutRule";
    }

    private String text(LogEntryDto entry) {
        return (entry.getStackTrace() == null ? "" : entry.getStackTrace()) + " " + (entry.getMessage() == null ? "" : entry.getMessage());
    }
}
