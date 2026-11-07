package com.faultlens.analyzer.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.faultlens.analyzer.model.Analysis;
import com.faultlens.analyzer.repository.AnalysisRepository;
import com.faultlens.analyzer.service.AnalyzerService;
import com.faultlens.common.dto.LogEntryDto;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class LogErrorConsumerTest {
    private AnalyzerService analyzer;
    private AnalysisRepository repository;
    private ValueOperations<String, String> values;
    private LogErrorConsumer consumer;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        analyzer = mock(AnalyzerService.class);
        repository = mock(AnalysisRepository.class);
        values = mock(ValueOperations.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.opsForValue()).thenReturn(values);
        when(repository.findFirstByLogGroupIdOrderByAnalyzedAtDesc(any())).thenReturn(Optional.empty());
        consumer = new LogErrorConsumer(analyzer, repository, new AnalysisThrottle(redis));
    }

    @Test
    void concurrentErrorsOfOneGroupAreAnalysedOnce() throws Exception {
        // Behaves like Redis SET NX: only the first claim per key succeeds.
        ConcurrentHashMap<String, String> store = new ConcurrentHashMap<>();
        when(values.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenAnswer(inv -> store.putIfAbsent(inv.getArgument(0), inv.getArgument(1)) == null);

        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        for (int i = 0; i < 100; i++) {
            pool.submit(() -> {
                start.await();
                consumer.consume(entry(42L));
                return null;
            });
        }
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

        verify(analyzer, times(1)).analyze(any());
        verify(values, times(100)).setIfAbsent(eq("faultlens:analysis:group:42"), eq("1"), eq(Duration.ofHours(1)));
    }

    @Test
    void groupAnalysedWithinTheLastHourIsSkippedWithoutTouchingRedis() {
        Analysis recent = new Analysis();
        recent.setAnalyzedAt(Instant.now().minusSeconds(600));
        when(repository.findFirstByLogGroupIdOrderByAnalyzedAtDesc(7L)).thenReturn(Optional.of(recent));

        consumer.consume(entry(7L));

        verify(analyzer, never()).analyze(any());
        verify(values, never()).setIfAbsent(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void redisOutageFallsBackToTheDatabaseCheck() {
        when(values.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenThrow(new RedisConnectionFailureException("down"));

        consumer.consume(entry(9L));

        verify(analyzer).analyze(any());
    }

    @Test
    void entryWithoutGroupIsAnalysed() {
        consumer.consume(entry(null));

        verify(analyzer).analyze(any());
        verify(values, never()).setIfAbsent(anyString(), anyString(), any(Duration.class));
    }

    private static LogEntryDto entry(Long groupId) {
        return LogEntryDto.builder().groupId(groupId).message("boom").build();
    }
}
