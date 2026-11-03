package com.faultlens.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.faultlens.api.model.Alarm;
import com.faultlens.api.model.AlarmStatus;
import com.faultlens.api.repository.AlarmRepository;
import com.faultlens.common.dto.AnalysisResultDto;
import com.faultlens.common.exception.FaultLensException;
import com.faultlens.common.exception.ResourceNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

class AlarmServiceTest {
    private AlarmRepository repository;
    private AlarmService service;

    @BeforeEach
    void setUp() {
        repository = mock(AlarmRepository.class);
        service = new AlarmService(repository);
        when(repository.save(any(Alarm.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @SuppressWarnings("unchecked")
    void listReturnsNewestFirstPage() {
        Alarm alarm = alarm(1L, AlarmStatus.ACTIVE);
        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(alarm), PageRequest.of(0, 20), 41));

        var page = service.list(AlarmStatus.ACTIVE, "critical", 0, 20);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAll(any(Specification.class), pageable.capture());
        assertThat(pageable.getValue().getSort().getOrderFor("triggeredAt").getDirection())
                .isEqualTo(Sort.Direction.DESC);
        assertThat(page.getContent()).singleElement()
                .satisfies(dto -> assertThat(dto.getStatus()).isEqualTo(AlarmStatus.ACTIVE));
        assertThat(page.getTotalElements()).isEqualTo(41);
        assertThat(page.getTotalPages()).isEqualTo(3);
    }

    @Test
    void statsCountsPerStatusAndOpenCritical() {
        when(repository.countByStatus(AlarmStatus.ACTIVE)).thenReturn(4L);
        when(repository.countByStatus(AlarmStatus.ACKNOWLEDGED)).thenReturn(2L);
        when(repository.countByStatus(AlarmStatus.RESOLVED)).thenReturn(9L);
        when(repository.countByStatusInAndSeverity(anyCollection(), eq("CRITICAL"))).thenReturn(3L);

        var stats = service.stats();

        assertThat(stats.active()).isEqualTo(4);
        assertThat(stats.acknowledged()).isEqualTo(2);
        assertThat(stats.resolved()).isEqualTo(9);
        assertThat(stats.openCritical()).isEqualTo(3);
    }

    @Test
    void acknowledgeMovesActiveAlarmToAcknowledged() {
        when(repository.findById(1L)).thenReturn(Optional.of(alarm(1L, AlarmStatus.ACTIVE)));

        assertThat(service.acknowledge(1L).getStatus()).isEqualTo(AlarmStatus.ACKNOWLEDGED);
    }

    @Test
    void acknowledgeRejectsResolvedAlarm() {
        when(repository.findById(1L)).thenReturn(Optional.of(alarm(1L, AlarmStatus.RESOLVED)));

        assertThatThrownBy(() -> service.acknowledge(1L))
                .isInstanceOf(FaultLensException.class)
                .hasFieldOrPropertyWithValue("errorCode", "ALARM_ALREADY_RESOLVED");
    }

    @Test
    void resolveSetsStatusAndKeepsFirstResolutionTime() {
        Alarm alarm = alarm(1L, AlarmStatus.ACKNOWLEDGED);
        when(repository.findById(1L)).thenReturn(Optional.of(alarm));

        Instant first = service.resolveAlarm(1L).getResolvedAt();
        Instant second = service.resolveAlarm(1L).getResolvedAt();

        assertThat(alarm.getStatus()).isEqualTo(AlarmStatus.RESOLVED);
        assertThat(first).isNotNull().isEqualTo(second);
    }

    @Test
    void missingAlarmIsNotFoundForEveryOperation() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolveAlarm(99L)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.acknowledge(99L)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.deleteAlarm(99L)).isInstanceOf(ResourceNotFoundException.class);
        verify(repository, never()).delete(any(Alarm.class));
    }

    @Test
    void dispatcherStoresSourceIdNotLogGroupId() {
        AlarmDispatcherService dispatcher = new AlarmDispatcherService(
                repository,
                mock(com.faultlens.api.repository.NotificationSettingsRepository.class),
                mock(com.faultlens.api.repository.UserAccountRepository.class),
                mock(SlackNotificationService.class),
                mock(EmailNotificationService.class),
                mock(InAppNotificationService.class));
        AnalysisResultDto analysis = new AnalysisResultDto();
        analysis.setId(5L);
        analysis.setLogGroupId(77L);
        analysis.setSourceId(3L);

        dispatcher.dispatchAlarm(analysis);

        ArgumentCaptor<Alarm> saved = ArgumentCaptor.forClass(Alarm.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getSourceId()).isEqualTo(3L);
        assertThat(saved.getValue().getStatus()).isEqualTo(AlarmStatus.ACTIVE);
    }

    private static Alarm alarm(Long id, AlarmStatus status) {
        return Alarm.builder()
                .id(id).ruleName("rule").severity("CRITICAL").status(status)
                .message("m").triggeredAt(Instant.now()).build();
    }
}
