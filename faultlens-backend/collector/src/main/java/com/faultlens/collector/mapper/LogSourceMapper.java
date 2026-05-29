package com.faultlens.collector.mapper;

import com.faultlens.collector.dto.LogSourceCreateRequest;
import com.faultlens.collector.dto.LogSourceUpdateRequest;
import com.faultlens.collector.model.LogSource;
import org.springframework.stereotype.Component;

@Component
public class LogSourceMapper {
    /**
     * Converts a create request to a source entity.
     */
    public LogSource toEntity(LogSourceCreateRequest request) {
        return LogSource.builder()
                .name(request.getName())
                .type(request.getType())
                .config(request.getConfig())
                .enabled(request.isEnabled())
                .build();
    }

    /**
     * Applies mutable fields from an update request.
     */
    public void updateEntity(LogSource target, LogSourceUpdateRequest request) {
        target.setName(request.getName());
        target.setType(request.getType());
        target.setConfig(request.getConfig());
        target.setEnabled(request.isEnabled());
    }
}
