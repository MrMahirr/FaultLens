package com.faultlens.collector.adapter;

import com.faultlens.common.dto.LogEventDto;
import java.util.function.Consumer;

/**
 * Where an adapter delivers a source's events and reports trouble (P08). The orchestrator's sink
 * records errors as the source's health (shown on the Sources page); plain lambdas ignore them.
 */
@FunctionalInterface
public interface SourceSink extends Consumer<LogEventDto> {
    /** The adapter cannot collect right now (it may retry). */
    default void error(String message) {
    }

    default void error(Throwable error) {
        error(error.getClass().getSimpleName() + (error.getMessage() == null ? "" : ": " + error.getMessage()));
    }
}
