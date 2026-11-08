package com.faultlens.api.dto;

import com.faultlens.common.dto.LogEntryDto;
import java.util.List;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * The entries around one entry of the same source and pod (P10 / 2.3), oldest first, the anchor
 * included. {@code has_more_before/after}: there are more within the window; load them by asking
 * for the context of the first / last entry.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record LogContextDto(Long anchorId, List<LogEntryDto> entries, boolean hasMoreBefore, boolean hasMoreAfter,
                            int windowMinutes) {
}
