package com.faultlens.api.service;

import com.faultlens.api.model.LogGroupEventType;
import com.faultlens.common.enums.GroupStatus;
import com.faultlens.common.exception.ConflictException;
import com.faultlens.common.exception.FaultLensException;
import java.time.Instant;

/**
 * Manual log group transitions (P10 / 2.1, ADR 0004). The system transitions (regression, ignore
 * expiry) happen in SQL in the processor's {@code LogGroupingService}.
 *
 * <pre>
 *   UNRESOLVED -> RESOLVED | IGNORED
 *   RESOLVED   -> UNRESOLVED (reopen) | IGNORED
 *   IGNORED    -> UNRESOLVED (reopen) | RESOLVED | IGNORED (only with a different period)
 * </pre>
 * Any other "transition" is a no-op the client did not mean (stale screen, double click): 409.
 */
public final class GroupStateMachine {
    private GroupStateMachine() {
    }

    /**
     * Checks a manual change and returns the timeline event it records.
     *
     * @throws ConflictException   the group is already in that status (409)
     * @throws FaultLensException  {@code ignoreUntil} without IGNORED, or not in the future (400)
     */
    public static LogGroupEventType transition(GroupStatus from, GroupStatus to, Instant ignoreUntil, Instant now) {
        return transition(from, to, ignoreUntil, null, now);
    }

    /**
     * As {@link #transition(GroupStatus, GroupStatus, Instant, Instant)}, knowing the group's
     * current {@code ignore_until}: IGNORED -> IGNORED is only a change with a different period
     * (409 otherwise, so a double click does not rewrite who ignored it and when).
     */
    public static LogGroupEventType transition(GroupStatus from, GroupStatus to, Instant ignoreUntil,
                                               Instant currentIgnoreUntil, Instant now) {
        if (from == GroupStatus.IGNORED && to == GroupStatus.IGNORED
                && java.util.Objects.equals(micros(ignoreUntil), micros(currentIgnoreUntil))) {
            throw new ConflictException("Log group is already ignored for that period", "GROUP_STATUS_UNCHANGED");
        }
        if (ignoreUntil != null) {
            if (to != GroupStatus.IGNORED) {
                throw new FaultLensException("ignore_until is only allowed with status IGNORED", "INVALID_GROUP_STATUS");
            }
            if (!ignoreUntil.isAfter(now)) {
                throw new FaultLensException("ignore_until must be in the future", "INVALID_GROUP_STATUS");
            }
        }
        if (from == to && to != GroupStatus.IGNORED) {
            throw new ConflictException("Log group is already " + to, "GROUP_STATUS_UNCHANGED");
        }
        return switch (to) {
            case RESOLVED -> LogGroupEventType.RESOLVED;
            case IGNORED -> LogGroupEventType.IGNORED;
            case UNRESOLVED -> LogGroupEventType.REOPENED;
        };
    }

    private static Instant micros(Instant instant) {
        return instant == null ? null : instant.truncatedTo(java.time.temporal.ChronoUnit.MICROS);
    }
}
