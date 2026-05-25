package com.loglens.common.enums;

import lombok.Getter;

@Getter
public enum Severity {
    TRACE(0),
    DEBUG(1),
    INFO(2),
    WARN(3),
    ERROR(4),
    CRITICAL(5);

    private final int level;

    Severity(int level) {
        this.level = level;
    }

    /**
     * Returns the higher severity by numeric level.
     */
    public static Severity max(Severity first, Severity second) {
        if (first == null) {
            return second == null ? INFO : second;
        }
        if (second == null) {
            return first;
        }
        return first.level >= second.level ? first : second;
    }

    /**
     * Parses a severity value case-insensitively, returning INFO for blank input.
     */
    public static Severity fromString(String value) {
        if (value == null || value.isBlank()) {
            return INFO;
        }
        for (Severity severity : values()) {
            if (severity.name().equalsIgnoreCase(value.trim())) {
                return severity;
            }
        }
        throw new IllegalArgumentException("Unknown severity: " + value);
    }
}
