package com.faultlens.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.faultlens.api.alarm.AlarmRulesChangedEvent;
import com.faultlens.api.dto.AlarmRuleDtos.AlarmRuleRequest;
import com.faultlens.api.model.AlarmChannel;
import com.faultlens.api.model.AlarmRule;
import com.faultlens.api.model.AlarmRuleType;
import com.faultlens.api.repository.AlarmRuleRepository;
import com.faultlens.common.enums.Severity;
import com.faultlens.common.exception.FaultLensException;
import com.faultlens.common.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class AlarmRuleServiceTest {
    private AlarmRuleRepository repository;
    private ApplicationEventPublisher events;
    private AlarmRuleService service;

    @BeforeEach
    void setUp() {
        repository = mock(AlarmRuleRepository.class);
        events = mock(ApplicationEventPublisher.class);
        when(repository.save(any(AlarmRule.class))).thenAnswer(inv -> {
            AlarmRule rule = inv.getArgument(0);
            if (rule.getId() == null) {
                rule.setId(1L);
            }
            return rule;
        });
        service = new AlarmRuleService(repository, events);
    }

    @Test
    void createsAThresholdRuleWithDefaultsAndAnnouncesTheChange() {
        var created = service.create(new AlarmRuleRequest("Hata patlaması", AlarmRuleType.THRESHOLD, Severity.ERROR,
                300, 50, "ignored for threshold", List.of(3L, 3L), null, null, null), "admin");

        assertThat(created.windowSec()).isEqualTo(300);
        assertThat(created.threshold()).isEqualTo(50);
        assertThat(created.pattern()).as("not kept for other types").isNull();
        assertThat(created.sourceIds()).containsExactly(3L);
        assertThat(created.channels()).containsExactly(AlarmChannel.IN_APP);
        assertThat(created.cooldownSec()).isEqualTo(900);
        assertThat(created.enabled()).isTrue();
        assertThat(created.createdBy()).isEqualTo("admin");
        verify(events).publishEvent(any(AlarmRulesChangedEvent.class));
    }

    @Test
    void thresholdRulesNeedWindowAndThreshold() {
        assertThatThrownBy(() -> service.create(new AlarmRuleRequest("x", AlarmRuleType.THRESHOLD, Severity.ERROR,
                null, 5, null, null, null, null, null), "admin"))
                .isInstanceOf(FaultLensException.class)
                .hasFieldOrPropertyWithValue("errorCode", "INVALID_ALARM_RULE");
        verify(repository, never()).save(any());
    }

    @Test
    void patternRulesNeedAValidRegex() {
        assertThatThrownBy(() -> service.create(new AlarmRuleRequest("x", AlarmRuleType.PATTERN, Severity.ERROR,
                null, null, "Connection (refused", null, null, null, null), "admin"))
                .isInstanceOf(FaultLensException.class)
                .hasMessageContaining("Invalid pattern");
        assertThatThrownBy(() -> service.create(new AlarmRuleRequest("x", AlarmRuleType.PATTERN, Severity.ERROR,
                null, null, "  ", null, null, null, null), "admin"))
                .isInstanceOf(FaultLensException.class);
    }

    @Test
    void togglingAndDeletingAnnounceTheChange() {
        AlarmRule rule = AlarmRule.builder().id(5L).name("r").type(AlarmRuleType.NEW_GROUP).severity(Severity.ERROR).build();
        when(repository.findById(5L)).thenReturn(Optional.of(rule));

        assertThat(service.setEnabled(5L, false).enabled()).isFalse();
        service.delete(5L);

        verify(repository).delete(rule);
        verify(events, org.mockito.Mockito.times(2)).publishEvent(any(AlarmRulesChangedEvent.class));
    }

    @Test
    void unknownRuleIsNotFound() {
        when(repository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(9L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
