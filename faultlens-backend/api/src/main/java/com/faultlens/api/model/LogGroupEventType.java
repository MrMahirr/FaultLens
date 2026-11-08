package com.faultlens.api.model;

/**
 * Kinds of {@code log_group_events} rows (P10 / 2.1, ADR 0004). CREATED, REGRESSED and UNIGNORED
 * are written by the processor (LogGroupingService), the rest by {@code LogGroupLifecycleService}.
 * 2.2 collaboration events are written by {@code LogGroupCollaborationService}.
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
    UNIGNORED,
    /** Assigned to {@code assignee} (2.2). */
    ASSIGNED,
    /** Assignment removed. */
    UNASSIGNED,
    /** A comment was added. */
    COMMENTED,
    /** This group was merged into {@code related_group_id}; its new entries go there. */
    MERGED,
    /** {@code related_group_id} was merged into this group. */
    MERGED_FROM,
    /** A merge with {@code related_group_id} was undone. */
    UNMERGED
}
