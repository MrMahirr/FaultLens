package com.faultlens.processor.parser;

import com.faultlens.common.dto.LogEventDto;
import com.faultlens.common.enums.Severity;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Spring Boot (Logback) console lines, in both layouts:
 * <ul>
 *   <li>Boot 2/early 3: {@code 2024-01-15 14:23:01.123 ERROR 123 --- [main] c.e.Service : msg}</li>
 *   <li>Boot 3.2+: {@code 2026-06-05T19:02:36.647Z  WARN 1 --- [app] [nio-exec-7] c.e.Service : msg}
 *       (ISO timestamp with offset, optional application name before the thread)</li>
 * </ul>
 * The level comes from the line itself; text after the first line (an attached stack trace) is
 * kept as the stack trace. Runs before {@link JavaStackTraceParser} so a WARN line that carries an
 * exception stays WARN.
 */
@Component
public class SpringBootLogParser implements LogParser {
    private static final Pattern SPRING_BOOT = Pattern.compile(
            "^(\\d{4}-\\d{2}-\\d{2}[ T]\\d{2}:\\d{2}:\\d{2}[.,]\\d{3,9}(?:Z|[+-]\\d{2}:?\\d{2})?)"
                    + "\\s+(ERROR|WARN|INFO|DEBUG|TRACE)"
                    + "\\s+(?:\\d+\\s+---\\s+)?"
                    + "(?:\\[([^\\]]*)]\\s*)?"
                    + "(?:\\[([^\\]]*)]\\s*)?"
                    + "([^\\s:][^:]*?)?\\s*:\\s(.*)$");

    /**
     * Detects Spring Boot log lines (first line of the event).
     */
    @Override
    public boolean supports(LogEventDto event) {
        String firstLine = firstLine(text(event));
        return firstLine != null && SPRING_BOOT.matcher(firstLine).matches();
    }

    /**
     * Parses severity, application, thread, logger and message from a Spring Boot line.
     */
    @Override
    public ParsedLog parse(LogEventDto event) {
        String text = text(event);
        String firstLine = firstLine(text);
        Matcher matcher = firstLine == null ? null : SPRING_BOOT.matcher(firstLine);
        if (matcher == null || !matcher.matches()) {
            return new ParsedLog(text, null, Severity.INFO, Map.of("format", "spring-boot"));
        }
        // One bracket group is the thread; two are [application] [thread].
        String first = trim(matcher.group(3));
        String second = matcher.group(4);
        String application = second == null ? "" : first;
        String thread = second == null ? first : trim(second);
        String logger = trim(matcher.group(5));
        String message = trim(matcher.group(6));
        String rest = text.length() > firstLine.length() ? text.substring(firstLine.length()).strip() : "";
        return new ParsedLog(message, rest.isEmpty() ? null : rest, Severity.fromString(matcher.group(2)), Map.of(
                "format", "spring-boot",
                "application", application,
                "thread", thread,
                "logger", logger));
    }

    /**
     * Spring Boot lines are recognised first: their level is authoritative.
     */
    @Override
    public int getPriority() {
        return 5;
    }

    private static String text(LogEventDto event) {
        return event.getRawLine() != null ? event.getRawLine() : event.getMessage();
    }

    private static String firstLine(String text) {
        if (text == null) {
            return null;
        }
        int newline = text.indexOf('\n');
        return (newline < 0 ? text : text.substring(0, newline)).stripTrailing();
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
