package com.loglens.collector.adapter;

import com.loglens.collector.model.LogSource;
import com.loglens.common.dto.LogEventDto;
import com.loglens.common.enums.LogSourceType;
import java.util.function.Consumer;

public interface LogSourceAdapter {
    /**
     * Returns the source type supported by this adapter.
     */
    LogSourceType getType();

    /**
     * Starts streaming source logs to the callback.
     */
    void startStreaming(LogSource source, Consumer<LogEventDto> onEvent);

    /**
     * Stops streaming for a source id.
     */
    void stopStreaming(String sourceId);

    /**
     * Tests whether the source can be reached.
     */
    boolean testConnection(LogSource source);
}
