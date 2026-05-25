package com.loglens.processor.parser;

import com.loglens.common.dto.LogEventDto;
import com.loglens.common.enums.Severity;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class GenericLogParser implements LogParser {
    /**
     * Generic parser supports every event.
     */
    @Override
    public boolean supports(LogEventDto event) {
        return true;
    }

    /**
     * Classifies logs by common severity keywords.
     */
    @Override
    public ParsedLog parse(LogEventDto event) {
        String text = event.getRawLine() != null ? event.getRawLine() : event.getMessage();
        String upper = text == null ? "" : text.toUpperCase(Locale.ROOT);
        Severity severity = upper.contains("CRITICAL") || upper.contains("FATAL") ? Severity.CRITICAL
                : upper.contains("ERROR") ? Severity.ERROR
                : upper.contains("WARN") ? Severity.WARN
                : upper.contains("DEBUG") ? Severity.DEBUG
                : Severity.INFO;
        return new ParsedLog(text, null, severity, Map.of("format", "generic"));
    }

    /**
     * Generic parser is the fallback parser.
     */
    @Override
    public int getPriority() {
        return 999;
    }
}
