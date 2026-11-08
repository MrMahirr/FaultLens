package com.faultlens.api.model;

/**
 * Kinds of {@code log_group_events} rows (P10 / 2.1, ADR 0004). CREATED, REGRESSED and UNIGNORED
 * are written by the processor (LogGroupingService), the rest by {@code LogGroupLifecycleService}.
 * 2.2 adds ASSIGNED, COMMENTED and MERGED.
 */
public enum LogGroupEventType {
    /** First entry of a new group. */
    CREATED,
    /** Marked resolved by a user. */
    RESOLVED,
    /** Muted by a user, optionally until a time. */
    IGNORED,
    /** Set back to unresolved by a user. */
    REOPENED,
    /** A resolved group was seen again; reopened by the system. */
    REGRESSED,
    /** An ignore period ran out and a new entry reopened the group. */
    UNIGNORED
}
