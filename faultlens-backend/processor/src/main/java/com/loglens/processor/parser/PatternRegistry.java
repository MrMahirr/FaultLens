package com.loglens.processor.parser;

import com.loglens.common.dto.LogEventDto;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PatternRegistry implements InitializingBean {
    private final List<LogParser> parsers;
    private List<LogParser> orderedParsers;

    /**
     * Returns parser beans ordered with generic parser last.
     */
    public List<LogParser> getParsers() {
        return orderedParsers;
    }

    /**
     * Selects the first parser supporting the event.
     */
    public LogParser getParser(LogEventDto event) {
        return getParsers().stream()
                .filter(parser -> parser.supports(event))
                .findFirst()
                .orElseGet(() -> getParsers().stream()
                        .filter(GenericLogParser.class::isInstance)
                        .findFirst()
                        .orElseThrow());
    }

    /**
     * Selects the first parser supporting the event.
     */
    public LogParser selectParser(LogEventDto event) {
        return getParser(event);
    }

    /**
     * Sorts parser beans once the registry is initialized.
     */
    @Override
    public void afterPropertiesSet() {
        this.orderedParsers = parsers.stream()
                .sorted(Comparator.comparingInt(LogParser::getPriority))
                .toList();
    }
}
