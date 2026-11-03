package com.faultlens.collector.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.faultlens.collector.adapter.LocalFilePathPolicy;
import com.faultlens.collector.dto.LogSourceCreateRequest;
import com.faultlens.collector.dto.LogSourceUpdateRequest;
import com.faultlens.collector.mapper.LogSourceMapper;
import com.faultlens.collector.model.LogSource;
import com.faultlens.collector.repository.LogSourceRepository;
import com.faultlens.common.enums.LogSourceType;
import com.faultlens.common.exception.CollectorException;
import com.faultlens.common.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LogSourceServiceTest {
    private LogSourceRepository repository;
    private CollectorOrchestrator orchestrator;
    private LogSourceMapper mapper;
    private LogSourceService service;
    private LocalFilePathPolicy pathPolicy;

    @BeforeEach
    void setUp() {
        repository = mock(LogSourceRepository.class);
        orchestrator = mock(CollectorOrchestrator.class);
        mapper = mock(LogSourceMapper.class);
        pathPolicy = mock(LocalFilePathPolicy.class);
        service = new LogSourceService(repository, orchestrator, mapper, pathPolicy);
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

    @Test
    void updateKeepsStoredSecretWhenClientSendsMaskBack() {
        LogSource existing = LogSource.builder()
                .id(7L).name("ssh").type(LogSourceType.SSH)
                .config("{\"host\":\"10.0.0.5\",\"username\":\"deploy\",\"password\":\"s3cret!\"}")
                .enabled(false).build();
        when(repository.findById(7L)).thenReturn(Optional.of(existing));
        when(repository.save(any(LogSource.class))).thenAnswer(inv -> inv.getArgument(0));
        doCallRealMethod().when(mapper).updateEntity(any(LogSource.class), any(LogSourceUpdateRequest.class));

        service.update(7L, LogSourceUpdateRequest.builder()
                .name("ssh").type(LogSourceType.SSH).enabled(false)
                .config("{\"host\":\"10.0.0.6\",\"username\":\"deploy\",\"password\":\"••••••••\"}")
                .build());

        assertThat(existing.getConfig()).contains("\"password\":\"s3cret!\"").contains("10.0.0.6");
    }

    @Test
    void updateStoresNewSecretWhenClientSendsOne() {
        LogSource existing = LogSource.builder()
                .id(7L).name("ssh").type(LogSourceType.SSH)
                .config("{\"password\":\"old\"}").enabled(false).build();
        when(repository.findById(7L)).thenReturn(Optional.of(existing));
        when(repository.save(any(LogSource.class))).thenAnswer(inv -> inv.getArgument(0));
        doCallRealMethod().when(mapper).updateEntity(any(LogSource.class), any(LogSourceUpdateRequest.class));

        service.update(7L, LogSourceUpdateRequest.builder()
                .name("ssh").type(LogSourceType.SSH).enabled(false)
                .config("{\"password\":\"new-secret\"}").build());

        assertThat(existing.getConfig()).contains("new-secret").doesNotContain("old");
    }

    @Test
    void createRejectsLocalFileOutsideAllowedDirectories() {
        when(pathPolicy.requireAllowed("/etc/passwd"))
                .thenThrow(new CollectorException("outside", "LOCAL_FILE_PATH_NOT_ALLOWED"));
        LogSource source = LogSource.builder()
                .name("passwd").type(LogSourceType.LOCAL_FILE)
                .config("{\"logFilePath\":\"/etc/passwd\"}").enabled(true).build();

        assertThatThrownBy(() -> service.create(source))
                .isInstanceOf(CollectorException.class)
                .hasFieldOrPropertyWithValue("errorCode", "LOCAL_FILE_PATH_NOT_ALLOWED");
        verify(repository, never()).save(any());
        verify(orchestrator, never()).startSource(any());
    }

    @Test
    void createDoesNotCheckPathPolicyForOtherSourceTypes() {
        LogSource source = LogSource.builder()
                .name("docker").type(LogSourceType.DOCKER).config("{}").enabled(false).build();
        when(repository.save(source)).thenReturn(source);

        service.create(source);

        verifyNoInteractions(pathPolicy);
    }
}
