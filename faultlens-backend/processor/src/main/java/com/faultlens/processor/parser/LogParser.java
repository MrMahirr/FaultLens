package com.faultlens.processor.parser;

import com.faultlens.common.dto.LogEventDto;

public interface LogParser {
    /**
     * Returns whether this parser supports the event.
     */
    boolean supports(LogEventDto event);

    /**
     * Parses a raw log event into a structured log.
     */
    ParsedLog parse(LogEventDto event);

    /**
     * Returns parser priority where lower values run first.
     */
    int getPriority();
}
