package com.faultlens.analyzer.rule;

import com.faultlens.analyzer.engine.AnalysisResult;
import com.faultlens.common.dto.LogEntryDto;
import org.springframework.stereotype.Component;

@Component
public class NullPointerRule implements AnalysisRule {
    /**
     * Matches NullPointerException traces.
     */
    @Override
    public boolean matches(LogEntryDto entry) {
        return text(entry).contains("NullPointerException");
    }

    /**
     * Explains likely null dereference root cause.
     */
    @Override
    public AnalysisResult analyze(LogEntryDto entry) {
        return new AnalysisResult(
                "Null referans erisimi. Nesne initialize edilmemis veya beklenmedik null deger dondurulmus.",
                "Stack trace'deki ilk uygulama satirini kontrol et: " + firstApplicationLine(entry),
                null,
                0.9,
                "RULE_BASED",
                getRuleName()
        );
    }

    /**
     * Returns rule priority.
     */
    @Override
    public int getPriority() {
        return 10;
    }

    /**
     * Returns stable rule name.
     */
    @Override
    public String getRuleName() {
        return "NullPointerRule";
    }

    private String firstApplicationLine(LogEntryDto entry) {
        String stackTrace = entry.getStackTrace() == null ? entry.getMessage() : entry.getStackTrace();
        return stackTrace == null ? "uygulama satiri bulunamadi" : stackTrace.lines()
                .map(String::trim)
                .filter(line -> line.startsWith("at ") && 
                                !line.startsWith("at java.") && 
                                !line.startsWith("at javax.") && 
                                !line.startsWith("at sun.") && 
                                !line.startsWith("at org.springframework.") && 
                                !line.startsWith("at org.apache."))
                .findFirst()
                .orElse("uygulama satiri bulunamadi");
    }

    private String text(LogEntryDto entry) {
        return (entry.getStackTrace() == null ? "" : entry.getStackTrace()) + " " + (entry.getMessage() == null ? "" : entry.getMessage());
    }
}
