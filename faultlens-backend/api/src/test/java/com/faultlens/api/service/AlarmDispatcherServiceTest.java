package com.faultlens.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.faultlens.api.model.Alarm;
import com.faultlens.api.model.AlarmStatus;
import com.faultlens.api.repository.AlarmRepository;
import com.faultlens.common.dto.AnalysisResultDto;
import com.faultlens.common.enums.Severity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

class AlarmDispatcherServiceTest {
    private AlarmRepository alarmRepository;
    private AlarmNotifier notifier;
    private AlarmDispatcherService dispatcher;

    @BeforeEach
    void setUp() {
        alarmRepository = mock(AlarmRepository.class);
        notifier = mock(AlarmNotifier.class);
        when(alarmRepository.save(any(Alarm.class))).thenAnswer(inv -> inv.getArgument(0));
        dispatcher = new AlarmDispatcherService(alarmRepository, notifier);
    }

    @Test
    void savesAnActiveAlarmCarryingTheAnalysisSource() {
        dispatcher.dispatchAlarm(analysis(0.9));

        Alarm alarm = savedAlarm();
        assertThat(alarm.getSourceId()).as("regression: used to receive the log group id").isEqualTo(5L);
        assertThat(alarm.getRuleId()).isEqualTo("AI_ANALYSIS");
        assertThat(alarm.getStatus()).isEqualTo(AlarmStatus.ACTIVE);
        assertThat(alarm.getTriggeredAt()).isNotNull();
        assertThat(alarm.getMessage())
                .contains("Grup ID: 42")
                .contains("Kök Neden: DB down")
                .contains("Öneri: restart pool")
                .contains("%90");
    }

    @Test
    void severityIsCriticalOnlyAboveEightyPercentConfidence() {
        dispatcher.dispatchAlarm(analysis(0.81));
        dispatcher.dispatchAlarm(analysis(0.8));
        dispatcher.dispatchAlarm(analysis(null));

        ArgumentCaptor<Alarm> captor = ArgumentCaptor.forClass(Alarm.class);
        verify(alarmRepository, times(3)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(Alarm::getSeverity).containsExactly(Severity.CRITICAL, Severity.ERROR, Severity.ERROR);
        assertThat(captor.getAllValues().get(2).getMessage()).contains("%0");
    }

    @Test
    void storesTheAlarmBeforeHandingNotificationsToTheAsyncNotifier() {
        dispatcher.dispatchAlarm(analysis(0.5));

        InOrder order = inOrder(alarmRepository, notifier);
        order.verify(alarmRepository).save(any(Alarm.class));
        order.verify(notifier).notifyAll(eq(AlarmDispatcherService.TITLE), contains("Grup ID: 42"));
    }

    private Alarm savedAlarm() {
        ArgumentCaptor<Alarm> captor = ArgumentCaptor.forClass(Alarm.class);
        verify(alarmRepository).save(captor.capture());
        return captor.getValue();
    }

    private static AnalysisResultDto analysis(Double confidence) {
        return AnalysisResultDto.builder()
                .id(99L)
                .logGroupId(42L)
                .sourceId(5L)
                .rootCause("DB down")
                .suggestion("restart pool")
                .confidenceScore(confidence)
                .engineType("RULE_BASED")
                .build();
    }
}
