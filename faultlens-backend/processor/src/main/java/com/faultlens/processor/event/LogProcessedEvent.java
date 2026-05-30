package com.faultlens.processor.event;

import com.faultlens.common.dto.LogEntryDto;
import java.util.List;

public record LogProcessedEvent(List<LogEntryDto> toErrorQueue, List<LogEntryDto> toPublish) {
}
