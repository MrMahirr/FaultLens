package com.faultlens.api.model;

/** Alarm rule kinds (ADR 0001). */
public enum AlarmRuleType {
    /** At least {@code threshold} matching entries in the last {@code windowSec}. */
    THRESHOLD,
    /** A matching entry whose message matches {@code pattern}. */
    PATTERN,
    /** A new log group (an error never seen before) of at least the rule's severity. */
    NEW_GROUP
}
