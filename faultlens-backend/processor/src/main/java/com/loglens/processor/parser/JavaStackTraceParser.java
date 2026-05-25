package com.loglens.processor.parser;

import com.loglens.common.dto.LogEventDto;
import com.loglens.common.enums.Severity;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class JavaStackTraceParser implements LogParser {
    /**
     * Detects Java exception and stack trace markers.
     */
    @Override
    public boolean supports(LogEventDto event) {
        String text = ((event.getRawLine() == null ? "" : event.getRawLine()) + "\n" + (event.getMessage() == null ? "" : event.getMessage()));
        return text.contains("Exception")
                || text.contains("Error")
                || text.contains("Caused by:")
                || text.contains("at ");
    }

    /**
     * Extracts the Java stack trace as-is and classifies as error.
     */
    @Override
    public ParsedLog parse(LogEventDto event) {
        String text = event.getMessage() != null && !event.getMessage().isBlank() ? event.getMessage() : event.getRawLine();
        String firstLine = text == null ? "" : text.lines().findFirst().orElse(text);
        return new ParsedLog(firstLine, text, Severity.ERROR, Map.of("format", "java-stacktrace"));
    }

    /**
     * Java stack traces should be parsed before generic formats.
     */
    @Override
    public int getPriority() {
        return 10;
    }
}
