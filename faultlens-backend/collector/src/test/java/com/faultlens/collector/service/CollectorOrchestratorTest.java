package com.faultlens.collector.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import com.faultlens.collector.adapter.LogSourceAdapter;
import com.faultlens.collector.kafka.LogEventProducer;
import com.faultlens.collector.model.LogSource;
import com.faultlens.collector.repository.LogSourceRepository;
import com.faultlens.common.enums.LogSourceType;
import com.faultlens.common.dto.LogEventDto;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CollectorOrchestratorTest {
    private LogSourceRepository repository;
    private LogEventProducer producer;
    private LogSourceAdapter mockAdapter;
    private CollectorOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        repository = mock(LogSourceRepository.class);
        producer = mock(LogEventProducer.class);
        mockAdapter = mock(LogSourceAdapter.class);
        when(mockAdapter.getType()).thenReturn(LogSourceType.SSH);
        
        orchestrator = new CollectorOrchestrator(repository, producer, List.of(mockAdapter));
    }

    @Test
    void startSourceIgnoresDisabledSource() {
        LogSource source = new LogSource();
        source.setId(1L);
        source.setEnabled(false);
        source.setType(LogSourceType.SSH);

        orchestrator.startSource(source);

        verify(mockAdapter, never()).startStreaming(any(), any());
    }

    @Test
    void startSourceIgnoresNullId() {
        LogSource source = new LogSource();
        source.setId(null);
        source.setEnabled(true);
        source.setType(LogSourceType.SSH);

        orchestrator.startSource(source);

        verify(mockAdapter, never()).startStreaming(any(), any());
    }

    @Test
    void startSourceSubmitsStreamingTask() throws InterruptedException {
        LogSource source = new LogSource();
        source.setId(1L);
        source.setEnabled(true);
        source.setType(LogSourceType.SSH);

        orchestrator.startSource(source);
        
        // Wait a short time for executor to process
        Thread.sleep(50);

        verify(mockAdapter).startStreaming(eq(source), any());
    }

    @Test
    void stopSourceCancelsTaskAndCallsAdapter() {
        LogSource source = new LogSource();
        source.setId(1L);
        source.setEnabled(true);
        source.setType(LogSourceType.SSH);

        when(repository.findById(1L)).thenReturn(Optional.of(source));

        // Start source to create running task
        orchestrator.startSource(source);
        
        // Stop source
        orchestrator.stopSource(1L);

        verify(mockAdapter).stopStreaming("1");
    }

    @Test
    void testSourceDelegatesToAdapter() {
        LogSource source = new LogSource();
        source.setId(1L);
        source.setType(LogSourceType.SSH);

        when(mockAdapter.testConnection(source)).thenReturn(true);

        boolean result = orchestrator.testSource(source);

        assertThat(result).isTrue();
        verify(mockAdapter).testConnection(source);
    }

    @Test
    void startEnabledSourcesStartsOnlyActiveSources() {
        LogSource source1 = new LogSource();
        source1.setId(1L);
        source1.setEnabled(true);
        source1.setType(LogSourceType.SSH);

        LogSource source2 = new LogSource();
        source2.setId(2L);
        source2.setEnabled(true);
        source2.setType(LogSourceType.SSH);

        when(repository.findByEnabledTrue()).thenReturn(List.of(source1, source2));

        orchestrator.startEnabledSources();

        verify(repository).findByEnabledTrue();
    }

    // ── lastSeenAt is batched (P06): no write per log line, no whole-entity save ──

    @Test
    void eventsAreForwardedWithoutAWritePerEventAndFlushedOncePerSource() throws Exception {
        LogSource source = enabledSource(1L);
        CountDownLatch delivered = new CountDownLatch(1);
        doAnswer(inv -> {
            Consumer<LogEventDto> onEvent = inv.getArgument(1);
            for (int i = 0; i < 50; i++) {
                onEvent.accept(LogEventDto.builder().message("line " + i).build());
            }
            delivered.countDown();
            return null;
        }).when(mockAdapter).startStreaming(eq(source), any());

        orchestrator.startSource(source);
        assertThat(delivered.await(5, TimeUnit.SECONDS)).isTrue();

        verify(producer, times(50)).send(any());
        verify(repository, never()).save(any());
        verify(repository, never()).touchLastSeenAt(anyLong(), any());
        assertThat(source.getLastSeenAt()).as("kept in memory as the adapter's resume point").isNotNull();

        orchestrator.flushLastSeen();
        orchestrator.flushLastSeen(); // nothing new → no second write

        verify(repository, times(1)).touchLastSeenAt(eq(1L), eq(source.getLastSeenAt()));
    }

    @Test
    void failedFlushIsRetriedOnTheNextOne() throws Exception {
        LogSource source = enabledSource(2L);
        CountDownLatch delivered = new CountDownLatch(1);
        doAnswer(inv -> {
            Consumer<LogEventDto> onEvent = inv.getArgument(1);
            onEvent.accept(LogEventDto.builder().message("x").build());
            delivered.countDown();
            return null;
        }).when(mockAdapter).startStreaming(eq(source), any());
        when(repository.touchLastSeenAt(eq(2L), any()))
                .thenThrow(new IllegalStateException("db down"))
                .thenReturn(1);

        orchestrator.startSource(source);
        assertThat(delivered.await(5, TimeUnit.SECONDS)).isTrue();
        orchestrator.flushLastSeen();
        orchestrator.flushLastSeen();

        verify(repository, times(2)).touchLastSeenAt(eq(2L), any(Instant.class));
    }

    private static LogSource enabledSource(Long id) {
        LogSource source = new LogSource();
        source.setId(id);
        source.setEnabled(true);
        source.setType(LogSourceType.SSH);
        return source;
    }
}
