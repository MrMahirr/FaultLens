package com.faultlens.processor.parser;

import com.faultlens.common.dto.LogEventDto;
import com.faultlens.common.enums.Severity;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class SpringBootLogParser implements LogParser {
    private static final Pattern SPRING_BOOT = Pattern.compile(
            "^(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3})\\s+(ERROR|WARN|INFO|DEBUG|TRACE)\\s+(?:\\d+\\s+---\\s+)?(?:\\[([^]]+)])?\\s*([^:]+)?\\s*:\\s*(.*)$");

    /**
     * Detects standard Spring Boot log lines.
     */
    @Override
    public boolean supports(LogEventDto event) {
        String text = text(event);
        return text != null && SPRING_BOOT.matcher(text).matches();
    }

    /**
     * Parses severity, thread, logger and message from a Spring Boot line.
     */
    @Override
    public ParsedLog parse(LogEventDto event) {
        String text = text(event);
        Matcher matcher = SPRING_BOOT.matcher(text);
        if (!matcher.matches()) {
            return new ParsedLog(text, null, Severity.INFO, Map.of("format", "spring-boot"));
        }
        String thread = matcher.group(3) == null ? "" : matcher.group(3).trim();
        String logger = matcher.group(4) == null ? "" : matcher.group(4).trim();
        String message = matcher.group(5) == null ? "" : matcher.group(5).trim();
        return new ParsedLog(message, null, Severity.fromString(matcher.group(2)), Map.of(
                "format", "spring-boot",
                "thread", thread,
                "logger", logger));
    }

    /**
     * Spring Boot parser runs after Java stack traces and before Nginx.
     */
    @Override
    public int getPriority() {
        return 15;
    }

    private String text(LogEventDto event) {
        return event.getRawLine() != null ? event.getRawLine() : event.getMessage();
    }
}
