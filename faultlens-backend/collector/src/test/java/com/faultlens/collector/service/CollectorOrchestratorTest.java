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
    private SourceLeaseManager leases;
    private SourceHealthReporter health;

    @BeforeEach
    void setUp() {
        repository = mock(LogSourceRepository.class);
        producer = mock(LogEventProducer.class);
        mockAdapter = mock(LogSourceAdapter.class);
        when(mockAdapter.getType()).thenReturn(LogSourceType.SSH);
        
        leases = mock(SourceLeaseManager.class);
        health = mock(SourceHealthReporter.class);
        when(leases.tryAcquire(anyLong())).thenReturn(true);
        when(leases.renew(anyLong())).thenReturn(true);
        orchestrator = new CollectorOrchestrator(repository, producer, List.of(mockAdapter), leases, health);
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

    // ── P08: leases, so several instances never collect the same source ──

    /** Adapter that blocks like a real stream until it is stopped. */
    private void adapterBlocksUntilStopped() {
        doAnswer(inv -> {
            try {
                Thread.sleep(60_000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            return null;
        }).when(mockAdapter).startStreaming(any(), any());
    }

    @Test
    void reconcileStartsOnlyTheSourcesThisInstanceWinsTheLeaseFor() {
        adapterBlocksUntilStopped();
        LogSource mine = enabledSource(1L);
        LogSource theirs = enabledSource(2L);
        when(repository.findByEnabledTrue()).thenReturn(List.of(mine, theirs));
        when(leases.tryAcquire(2L)).thenReturn(false); // another instance collects it

        orchestrator.reconcile();

        verify(mockAdapter, timeout(1000)).startStreaming(eq(mine), any());
        verify(mockAdapter, after(200).never()).startStreaming(eq(theirs), any());
        assertThat(orchestrator.isStreaming(1L)).isTrue();
        assertThat(orchestrator.isStreaming(2L)).isFalse();
        orchestrator.shutdown();
    }

    @Test
    void aSourceWhoseLeaseWasTakenOverStopsHere() {
        adapterBlocksUntilStopped();
        LogSource source = enabledSource(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(source));
        orchestrator.startSource(source);
        verify(leases).takeOver(1L);

        when(leases.renew(1L)).thenReturn(false); // edited on another instance
        orchestrator.heartbeat();

        verify(mockAdapter).stopStreaming("1");
        assertThat(orchestrator.isStreaming(1L)).isFalse();
    }

    @Test
    void reconcileStopsSourcesDisabledElsewhereAndHandsBackTheLease() {
        adapterBlocksUntilStopped();
        LogSource source = enabledSource(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(source));
        when(repository.findByEnabledTrue()).thenReturn(List.of(source));
        orchestrator.reconcile();
        verify(mockAdapter, timeout(1000)).startStreaming(eq(source), any());

        when(repository.findByEnabledTrue()).thenReturn(List.of());
        orchestrator.reconcile();

        verify(mockAdapter).stopStreaming("1");
        verify(leases).release(1L);
    }

    @Test
    void adapterErrorsAreReportedAsSourceHealth() {
        doAnswer(inv -> {
            com.faultlens.collector.adapter.SourceSink sink = inv.getArgument(1);
            sink.error("connection refused");
            throw new IllegalStateException("gave up");
        }).when(mockAdapter).startStreaming(any(), any());

        orchestrator.startSource(enabledSource(1L));

        verify(health, timeout(1000)).error(1L, "connection refused");
        verify(health, timeout(1000)).error(eq(1L), any(IllegalStateException.class));
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

        // the last line is sent when the stream ends (multi-line aggregator flush)
        verify(producer, timeout(2000).times(50)).send(any());
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
        verify(producer, timeout(2000)).send(any());
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
