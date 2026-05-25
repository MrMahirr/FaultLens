package com.loglens.processor.classifier;

import com.loglens.common.dto.LogEventDto;
import com.loglens.common.enums.Severity;
import com.loglens.processor.parser.ParsedLog;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class SeverityClassifier {
    /**
     * Chooses the higher severity between source and parser.
     */
    public Severity classify(LogEventDto event, ParsedLog parsedLog) {
        Severity severity = Severity.max(event.getSeverity(), parsedLog.severity());
        if (isStderr(event, parsedLog)) {
            return Severity.max(severity, Severity.WARN);
        }
        return severity;
    }

    private boolean isStderr(LogEventDto event, ParsedLog parsedLog) {
        String eventStream = event.getLabels() == null ? null : event.getLabels().get("stream");
        String parsedStream = parsedLog.metadata() == null ? null : parsedLog.metadata().get("stream");
        return isStderrValue(eventStream) || isStderrValue(parsedStream);
    }

    private boolean isStderrValue(String value) {
        return value != null && "STDERR".equals(value.trim().toUpperCase(Locale.ROOT));
    }
}
