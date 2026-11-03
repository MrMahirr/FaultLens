package com.faultlens.api.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * Alarm counters for the alarms page header (independent of list pagination).
 *
 * @param active       alarms in ACTIVE state
 * @param acknowledged alarms in ACKNOWLEDGED state
 * @param resolved     alarms in RESOLVED state
 * @param openCritical CRITICAL alarms that are not resolved yet
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record AlarmStatsDto(long active, long acknowledged, long resolved, long openCritical) {
}
