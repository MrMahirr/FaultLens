package com.faultlens.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.faultlens.api.model.LogGroupEventType;
import com.faultlens.common.enums.GroupStatus;
import com.faultlens.common.exception.ConflictException;
import com.faultlens.common.exception.FaultLensException;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class GroupStateMachineTest {
    private static final Instant NOW = Instant.parse("2026-11-08T12:00:00Z");

    @ParameterizedTest
    @CsvSource({
            "UNRESOLVED, RESOLVED,   RESOLVED",
            "UNRESOLVED, IGNORED,    IGNORED",
            "RESOLVED,   UNRESOLVED, REOPENED",
            "RESOLVED,   IGNORED,    IGNORED",
            "IGNORED,    UNRESOLVED, REOPENED",
            "IGNORED,    RESOLVED,   RESOLVED",
            "IGNORED,    IGNORED,    IGNORED",
    })
    void allowedTransitionsRecordTheirEvent(GroupStatus from, GroupStatus to, LogGroupEventType event) {
        assertThat(GroupStateMachine.transition(from, to, null, NOW)).isEqualTo(event);
    }

    @ParameterizedTest
    @CsvSource({"UNRESOLVED", "RESOLVED"})
    void settingTheSameStatusAgainIsAConflict(GroupStatus status) {
        assertThatThrownBy(() -> GroupStateMachine.transition(status, status, null, NOW))
                .isInstanceOf(ConflictException.class)
                .extracting("errorCode").isEqualTo("GROUP_STATUS_UNCHANGED");
    }

    @Test
    void ignoreUntilMustBeInTheFuture() {
        assertThat(GroupStateMachine.transition(GroupStatus.UNRESOLVED, GroupStatus.IGNORED,
                NOW.plus(Duration.ofHours(1)), NOW)).isEqualTo(LogGroupEventType.IGNORED);

        assertThatThrownBy(() -> GroupStateMachine.transition(GroupStatus.UNRESOLVED, GroupStatus.IGNORED, NOW, NOW))
                .isInstanceOf(FaultLensException.class)
                .isNotInstanceOf(ConflictException.class)
                .extracting("errorCode").isEqualTo("INVALID_GROUP_STATUS");
    }

    @Test
    void ignoreUntilIsOnlyForIgnored() {
        assertThatThrownBy(() -> GroupStateMachine.transition(GroupStatus.UNRESOLVED, GroupStatus.RESOLVED,
                NOW.plus(Duration.ofHours(1)), NOW))
                .isInstanceOf(FaultLensException.class)
                .extracting("errorCode").isEqualTo("INVALID_GROUP_STATUS");
    }
}
