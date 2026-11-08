package com.faultlens.processor.event;

import com.faultlens.common.enums.Severity;

/**
 * A RESOLVED log group was seen again and reopened (REGRESSION alarm rules, P10 / 2.1, ADR 0004).
 * Published inside the transaction that reopened the group; listeners should run after commit.
 *
 * @param regressionCount how many times the group has regressed, this one included
 */
public record LogGroupRegressedEvent(Long groupId, Severity severity, Long sourceId, String firstMessage,
                                     int regressionCount) {
}
