package com.loglens.common.enums;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SeverityTest {
    @Test
    void exposesExpectedNumericLevels() {
        assertThat(Severity.TRACE.getLevel()).isZero();
        assertThat(Severity.DEBUG.getLevel()).isEqualTo(1);
        assertThat(Severity.INFO.getLevel()).isEqualTo(2);
        assertThat(Severity.WARN.getLevel()).isEqualTo(3);
        assertThat(Severity.ERROR.getLevel()).isEqualTo(4);
        assertThat(Severity.CRITICAL.getLevel()).isEqualTo(5);
    }

    @Test
    void parsesSeverityCaseInsensitively() {
        assertThat(Severity.fromString("warn")).isEqualTo(Severity.WARN);
        assertThat(Severity.fromString(" ERROR ")).isEqualTo(Severity.ERROR);
    }

    @Test
    void defaultsBlankSeverityToInfo() {
        assertThat(Severity.fromString(null)).isEqualTo(Severity.INFO);
        assertThat(Severity.fromString(" ")).isEqualTo(Severity.INFO);
    }

    @Test
    void rejectsUnknownSeverity() {
        assertThatThrownBy(() -> Severity.fromString("fatal"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown severity");
    }

    @Test
    void returnsHigherSeverity() {
        assertThat(Severity.max(Severity.DEBUG, Severity.ERROR)).isEqualTo(Severity.ERROR);
        assertThat(Severity.max(null, null)).isEqualTo(Severity.INFO);
    }
}
