package com.loglens.collector.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.loglens.collector.dto.LogSourceCreateRequest;
import com.loglens.collector.dto.LogSourceUpdateRequest;
import com.loglens.collector.mapper.LogSourceMapper;
import com.loglens.collector.model.LogSource;
import com.loglens.collector.repository.LogSourceRepository;
import com.loglens.common.enums.LogSourceType;
import com.loglens.common.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LogSourceServiceTest {
    private LogSourceRepository repository;
    private CollectorOrchestrator orchestrator;
    private LogSourceMapper mapper;
    private LogSourceService service;

    @BeforeEach
    void setUp() {
        repository = mock(LogSourceRepository.class);
        orchestrator = mock(CollectorOrchestrator.class);
        mapper = mock(LogSourceMapper.class);
        service = new LogSourceService(repository, orchestrator, mapper);
    }

    @Test
    void createSourceStartsStreamWhenEnabled() {
        LogSource source = new LogSource();
        source.setId(1L);
        source.setEnabled(true);

        when(repository.save(any(LogSource.class))).thenReturn(source);

        LogSource result = service.create(source);

        assertThat(result).isNotNull();
        verify(repository).save(source);
        verify(orchestrator).startSource(source);
    }

    @Test
    void createSourceDoesNotStartStreamWhenDisabled() {
        LogSource source = new LogSource();
        source.setId(1L);
        source.setEnabled(false);

        when(repository.save(any(LogSource.class))).thenReturn(source);

        LogSource result = service.create(source);

        assertThat(result).isNotNull();
        verify(repository).save(source);
        verify(orchestrator, never()).startSource(any());
    }

    @Test
    void createFromRequestDelegatesToMapperAndSave() {
        LogSourceCreateRequest request = new LogSourceCreateRequest();
        LogSource source = new LogSource();
        source.setId(1L);
        source.setEnabled(true);

        when(mapper.toEntity(request)).thenReturn(source);
        when(repository.save(source)).thenReturn(source);

        LogSource result = service.create(request);

        assertThat(result).isNotNull();
        verify(mapper).toEntity(request);
        verify(orchestrator).startSource(source);
    }

    @Test
    void getThrowsResourceNotFoundExceptionWhenNotFound() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Log source not found: 1");
    }

    @Test
    void getReturnsSourceWhenFound() {
        LogSource source = new LogSource();
        source.setId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(source));

        LogSource result = service.get(1L);

        assertThat(result).isEqualTo(source);
    }

    @Test
    void deleteStopsSourceAndDeletesFromRepository() {
        LogSource source = new LogSource();
        source.setId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(source));

        service.delete(1L);

        verify(orchestrator).stopSource(1L);
        verify(repository).delete(source);
    }

    @Test
    void enableSetsEnabledAndStartsSource() {
        LogSource source = new LogSource();
        source.setId(1L);
        source.setEnabled(false);

        when(repository.findById(1L)).thenReturn(Optional.of(source));
        when(repository.save(any(LogSource.class))).thenAnswer(inv -> inv.getArgument(0));

        LogSource result = service.enable(1L);

        assertThat(result.isEnabled()).isTrue();
        verify(orchestrator).startSource(source);
    }

    @Test
    void disableSetsDisabledAndStopsSource() {
        LogSource source = new LogSource();
        source.setId(1L);
        source.setEnabled(true);

        when(repository.findById(1L)).thenReturn(Optional.of(source));
        when(repository.save(any(LogSource.class))).thenAnswer(inv -> inv.getArgument(0));

        LogSource result = service.disable(1L);

        assertThat(result.isEnabled()).isFalse();
        verify(orchestrator).stopSource(1L);
    }

    @Test
    void testConnectionDelegatesToOrchestrator() {
        LogSource source = new LogSource();
        source.setId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(source));
        when(orchestrator.testSource(source)).thenReturn(true);

        boolean result = service.testConnection(1L);

        assertThat(result).isTrue();
        verify(orchestrator).testSource(source);
    }
}
