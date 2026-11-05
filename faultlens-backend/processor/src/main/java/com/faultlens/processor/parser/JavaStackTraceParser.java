package com.faultlens.processor.parser;

import com.faultlens.common.dto.LogEventDto;
import com.faultlens.common.enums.Severity;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class JavaStackTraceParser implements LogParser {
    /** A fully qualified exception class at the start of a line, optionally after "Exception in thread". */
    private static final Pattern EXCEPTION_HEADER = Pattern.compile(
            "(?m)^(?:Exception in thread \"[^\"]*\" )?(?:[a-zA-Z_$][\\w$]*\\.)+[A-Z][\\w$]*(?:Exception|Error|Throwable)(?::|$)");
    /** A stack frame line: {@code at com.example.Foo.bar(Foo.java:42)}. */
    private static final Pattern STACK_FRAME = Pattern.compile("(?m)^\\s*at [\\w$.<>]+\\(");

    /**
     * Detects a real Java stack trace: an exception header, a stack frame or "Caused by:".
     * Plain words ("Error", "at ") in an ordinary message are not enough.
     */
    @Override
    public boolean supports(LogEventDto event) {
        String text = (event.getRawLine() == null ? "" : event.getRawLine()) + "\n"
                + (event.getMessage() == null ? "" : event.getMessage());
        return EXCEPTION_HEADER.matcher(text).find()
                || STACK_FRAME.matcher(text).find()
                || text.contains("\nCaused by: ");
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
