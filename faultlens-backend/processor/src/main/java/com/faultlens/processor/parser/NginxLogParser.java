package com.faultlens.processor.parser;

import com.faultlens.common.dto.LogEventDto;
import com.faultlens.common.enums.Severity;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class NginxLogParser implements LogParser {
    private static final Pattern COMBINED = Pattern.compile("^(\\S+) \\S+ \\S+ \\[[^]]+] \"([A-Z]+) ([^\"]+) HTTP/[^\"]+\" (\\d{3}) .*$");

    /**
     * Detects Nginx combined access log format.
     */
    @Override
    public boolean supports(LogEventDto event) {
        String text = event.getRawLine() != null ? event.getRawLine() : event.getMessage();
        return text != null && COMBINED.matcher(text).matches();
    }

    /**
     * Parses method, path and status code.
     */
    @Override
    public ParsedLog parse(LogEventDto event) {
        String text = event.getRawLine() != null ? event.getRawLine() : event.getMessage();
        Matcher matcher = COMBINED.matcher(text);
        if (!matcher.matches()) {
            return new ParsedLog(text, null, Severity.INFO, Map.of("format", "nginx"));
        }
        int status = Integer.parseInt(matcher.group(4));
        Severity severity = status >= 500 ? Severity.ERROR : status >= 400 ? Severity.WARN : Severity.INFO;
        String parsed = matcher.group(2) + " " + matcher.group(3) + " -> " + status;
        return new ParsedLog(parsed, null, severity, Map.of(
                "format", "nginx",
                "ip", matcher.group(1),
                "method", matcher.group(2),
                "path", matcher.group(3),
                "status", String.valueOf(status)));
    }

    /**
     * Nginx parser runs after application-specific parsers.
     */
    @Override
    public int getPriority() {
        return 20;
    }
}
