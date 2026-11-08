package com.faultlens.common.enums;

/**
 * Lifecycle of a log group (P10 / 2.1, ADR 0004).
 *
 * <ul>
 *   <li>{@link #UNRESOLVED}: default; the group needs attention.</li>
 *   <li>{@link #RESOLVED}: fixed. A new entry reopens it as a regression.</li>
 *   <li>{@link #IGNORED}: muted (no alarms), optionally until {@code ignore_until}; a new entry
 *       after that time reopens it, without a regression.</li>
 * </ul>
 */
public enum GroupStatus {
    UNRESOLVED,
    RESOLVED,
    IGNORED
}
