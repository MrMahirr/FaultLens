package com.faultlens.processor.parser;

import com.faultlens.common.dto.LogEventDto;
import com.faultlens.common.enums.Severity;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
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
        // Whole words only: "errorCount=0" or "warning-free" must not raise the level.
        Severity severity = hasWord(upper, "CRITICAL|FATAL") ? Severity.CRITICAL
                : hasWord(upper, "ERROR") ? Severity.ERROR
                : hasWord(upper, "WARN|WARNING") ? Severity.WARN
                : hasWord(upper, "DEBUG") ? Severity.DEBUG
                : Severity.INFO;
        return new ParsedLog(text, null, severity, Map.of("format", "generic"));
    }

    private static boolean hasWord(String upper, String alternatives) {
        return Pattern.compile("(?<![A-Z0-9_])(?:" + alternatives + ")(?![A-Z0-9_])").matcher(upper).find();
    }

    /**
     * Generic parser is the fallback parser.
     */
    @Override
    public int getPriority() {
        return 999;
    }
}
