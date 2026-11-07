package com.faultlens.collector.service;

import com.faultlens.common.dto.LogEventDto;
import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import java.util.regex.Pattern;

/**
 * Joins the lines of one multi-line log event (a stack trace, a message wrapped over several
 * lines) back into a single event before it is sent on. Adapters deliver one event per line; the
 * processor then stored every stack frame as a separate log entry.
 *
 * <p>A line continues the buffered event when it
 * <ul>
 *   <li>starts with whitespace ({@code \tat com.example...}), or</li>
 *   <li>starts with {@code Caused by:}, {@code Suppressed:} or {@code ... 12 more}, or</li>
 *   <li>has no timestamp prefix while the buffered event starts with one (Spring Boot, Logback,
 *       log4j layouts put a timestamp first; their wrapped text and exception headers don't).</li>
 * </ul>
 * Anything else starts a new event. The buffered event is sent when the next event starts, after
 * {@link #IDLE} without new lines ({@link #flushIfIdle}), when it reaches {@link #MAX_LINES} lines or
 * {@link #MAX_CHARS} characters, or on {@link #flush}. One instance per source, used by that source's
 * single streaming thread plus the idle flusher; methods are synchronized.
 */
public class MultilineAggregator implements Consumer<LogEventDto> {
    static final Duration IDLE = Duration.ofMillis(500);
    static final int MAX_LINES = 500;
    static final int MAX_CHARS = 64 * 1024;

    private static final Pattern TIMESTAMP_PREFIX = Pattern.compile(
            "^\\[?(\\d{4}-\\d{2}-\\d{2}[ T]\\d{2}:\\d{2}"         // ISO-8601 / Logback / log4j
                    + "|\\d{2}:\\d{2}:\\d{2}"                      // time only
                    + "|[A-Z][a-z]{2} +\\d{1,2} \\d{2}:\\d{2}:\\d{2})"); // syslog
    private static final Pattern CONTINUATION = Pattern.compile(
            "^(\\s|Caused by:|Suppressed:|\\.\\.\\. \\d+ (more|common frames omitted))");

    private final Consumer<LogEventDto> downstream;
    private final LongSupplier clock;

    private LogEventDto buffered;
    private StringBuilder message;
    private StringBuilder rawLine;
    private int lines;
    private boolean bufferedHasTimestamp;
    private long lastLineAt;

    public MultilineAggregator(Consumer<LogEventDto> downstream) {
        this(downstream, System::nanoTime);
    }

    MultilineAggregator(Consumer<LogEventDto> downstream, LongSupplier nanoClock) {
        this.downstream = downstream;
        this.clock = nanoClock;
    }

    @Override
    public synchronized void accept(LogEventDto event) {
        String line = event.getRawLine() != null ? event.getRawLine() : event.getMessage();
        if (buffered != null && line != null && continues(line)) {
            append(line);
            if (lines >= MAX_LINES || rawLine.length() >= MAX_CHARS) {
                flush();
            }
            return;
        }
        flush();
        start(event, line);
    }

    /**
     * Sends the buffered event, if any.
     */
    public synchronized void flush() {
        if (buffered == null) {
            return;
        }
        LogEventDto event = buffered;
        if (lines > 1) {
            event.setMessage(message.toString());
            event.setRawLine(rawLine.toString());
        }
        buffered = null;
        message = null;
        rawLine = null;
        lines = 0;
        downstream.accept(event);
    }

    /**
     * Sends the buffered event when no line arrived for {@link #IDLE}; called periodically.
     */
    public synchronized void flushIfIdle() {
        if (buffered != null && clock.getAsLong() - lastLineAt >= IDLE.toNanos()) {
            flush();
        }
    }

    private boolean continues(String line) {
        if (CONTINUATION.matcher(line).find()) {
            return true;
        }
        return bufferedHasTimestamp && !line.isBlank() && !TIMESTAMP_PREFIX.matcher(line).find();
    }

    private void start(LogEventDto event, String line) {
        buffered = event;
        message = new StringBuilder(event.getMessage() == null ? "" : event.getMessage());
        rawLine = new StringBuilder(line == null ? "" : line);
        lines = 1;
        bufferedHasTimestamp = line != null && TIMESTAMP_PREFIX.matcher(line).find();
        lastLineAt = clock.getAsLong();
    }

    private void append(String line) {
        message.append('\n').append(line);
        rawLine.append('\n').append(line);
        lines++;
        lastLineAt = clock.getAsLong();
    }
}
