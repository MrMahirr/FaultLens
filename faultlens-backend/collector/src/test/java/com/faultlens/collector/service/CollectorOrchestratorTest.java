package com.faultlens.collector.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.faultlens.collector.adapter.LogSourceAdapter;
import com.faultlens.collector.kafka.LogEventProducer;
import com.faultlens.collector.model.LogSource;
import com.faultlens.collector.repository.LogSourceRepository;
import com.faultlens.common.enums.LogSourceType;
import java.util.List;
import java.util.Optional;
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
}
