package com.faultlens.api.model;

/**
 * Alarm lifecycle: ACTIVE -> ACKNOWLEDGED (someone is on it) -> RESOLVED.
 */
public enum AlarmStatus {
    ACTIVE,
    ACKNOWLEDGED,
    RESOLVED
}
