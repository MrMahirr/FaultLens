package com.faultlens.api.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.faultlens.api.service.AlarmDispatcherService;
import com.faultlens.common.dto.AnalysisResultDto;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class AnalysisEventListenerTest {
    private AlarmDispatcherService dispatcher;
    private ValueOperations<String, String> values;
    private AnalysisEventListener listener;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        dispatcher = mock(AlarmDispatcherService.class);
        values = mock(ValueOperations.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.opsForValue()).thenReturn(values);
        listener = new AnalysisEventListener(dispatcher, new AlarmCooldown(redis, 15));
    }

    @Test
    void hundredAnalysesOfOneGroupRaiseOneAlarmPerCooldown() {
        ConcurrentHashMap<String, String> store = new ConcurrentHashMap<>();
        when(values.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenAnswer(inv -> store.putIfAbsent(inv.getArgument(0), inv.getArgument(1)) == null);

        for (int i = 0; i < 100; i++) {
            listener.consume(analysis(42L));
        }
        listener.consume(analysis(43L));

        verify(dispatcher, times(2)).dispatchAlarm(any());
        verify(values, times(100)).setIfAbsent(eq("faultlens:alarm:group:42"), eq("1"), eq(Duration.ofMinutes(15)));
        assertThat(store).containsOnlyKeys("faultlens:alarm:group:42", "faultlens:alarm:group:43");
    }

    @Test
    void redisOutageStillRaisesTheAlarm() {
        when(values.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenThrow(new RedisConnectionFailureException("down"));

        listener.consume(analysis(42L));

        verify(dispatcher).dispatchAlarm(any());
    }

    private static AnalysisResultDto analysis(Long groupId) {
        return AnalysisResultDto.builder().id(1L).logGroupId(groupId).confidenceScore(0.9).build();
    }
}
