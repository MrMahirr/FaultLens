package com.faultlens.processor.event;

import com.faultlens.common.dto.LogEntryDto;
import java.util.List;
import java.util.Set;

/**
 * One processed batch, published inside its transaction.
 *
 * @param mutedGroupIds groups that are IGNORED: their entries raise no alarms (P10 / 2.1)
 */
public record LogProcessedEvent(List<LogEntryDto> toErrorQueue, List<LogEntryDto> toPublish,
                                Set<Long> mutedGroupIds) {
    public LogProcessedEvent {
        mutedGroupIds = mutedGroupIds == null ? Set.of() : Set.copyOf(mutedGroupIds);
    }

    public LogProcessedEvent(List<LogEntryDto> toErrorQueue, List<LogEntryDto> toPublish) {
        this(toErrorQueue, toPublish, Set.of());
    }

    /** Whether alarms should skip this entry because its group is ignored. */
    public boolean isMuted(LogEntryDto entry) {
        return entry.getGroupId() != null && mutedGroupIds.contains(entry.getGroupId());
    }
}
