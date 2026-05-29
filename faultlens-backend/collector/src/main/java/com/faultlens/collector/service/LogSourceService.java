package com.faultlens.collector.service;

import com.faultlens.collector.dto.LogSourceCreateRequest;
import com.faultlens.collector.dto.LogSourceUpdateRequest;
import com.faultlens.collector.mapper.LogSourceMapper;
import com.faultlens.collector.model.LogSource;
import com.faultlens.collector.repository.LogSourceRepository;
import com.faultlens.common.enums.LogSourceType;
import com.faultlens.common.exception.ResourceNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class LogSourceService {
    private final LogSourceRepository repository;
    private final CollectorOrchestrator orchestrator;
    private final LogSourceMapper mapper;

    /**
     * Creates a log source from a request and starts streaming when enabled.
     */
    public LogSource create(LogSourceCreateRequest request) {
        return create(mapper.toEntity(request));
    }

    /**
     * Creates a log source and starts streaming when enabled.
     */
    public LogSource create(LogSource source) {
        LogSource saved = repository.save(source);
        if (saved.isEnabled()) {
            orchestrator.startSource(saved);
        }
        return saved;
    }

    /**
     * Updates a log source from a request and restarts streaming when needed.
     */
    public LogSource update(Long id, LogSourceUpdateRequest request) {
        LogSource existing = get(id);
        mapper.updateEntity(existing, request);
        return saveAndRestart(id, existing);
    }

    /**
     * Updates a log source and restarts streaming when needed.
     */
    public LogSource update(Long id, LogSource source) {
        LogSource existing = get(id);
        existing.setName(source.getName());
        existing.setType(source.getType());
        existing.setConfig(source.getConfig());
        existing.setEnabled(source.isEnabled());
        return saveAndRestart(id, existing);
    }

    /**
     * Deletes a log source and stops its stream.
     */
    public void delete(Long id) {
        orchestrator.stopSource(id);
        repository.delete(get(id));
    }

    /**
     * Returns all configured sources.
     */
    @Transactional(readOnly = true)
    public List<LogSource> list() {
        return repository.findAll();
    }

    /**
     * Returns all configured sources.
     */
    @Transactional(readOnly = true)
    public List<LogSource> findAll() {
        return list();
    }

    /**
     * Returns sources by type.
     */
    @Transactional(readOnly = true)
    public List<LogSource> findByType(LogSourceType type) {
        return repository.findByType(type);
    }

    /**
     * Returns a source by id.
     */
    @Transactional(readOnly = true)
    public LogSource get(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Log source not found: " + id));
    }

    /**
     * Returns a source by id.
     */
    @Transactional(readOnly = true)
    public LogSource findById(Long id) {
        return get(id);
    }

    /**
     * Enables a source and starts streaming.
     */
    public LogSource enable(Long id) {
        LogSource source = get(id);
        source.setEnabled(true);
        LogSource saved = repository.save(source);
        orchestrator.startSource(saved);
        return saved;
    }

    /**
     * Disables a source and stops streaming.
     */
    public LogSource disable(Long id) {
        LogSource source = get(id);
        source.setEnabled(false);
        orchestrator.stopSource(id);
        return repository.save(source);
    }

    /**
     * Tests source connectivity for an existing source.
     */
    @Transactional(readOnly = true)
    public boolean testConnection(Long id) {
        return orchestrator.testSource(get(id));
    }

    /**
     * Tests source connectivity before saving.
     */
    public boolean testConnection(LogSourceCreateRequest request) {
        LogSource tempSource = new LogSource();
        tempSource.setType(request.getType());
        tempSource.setConfig(request.getConfig());
        return orchestrator.testSource(tempSource);
    }

    private LogSource saveAndRestart(Long id, LogSource source) {
        LogSource saved = repository.save(source);
        orchestrator.stopSource(id);
        if (saved.isEnabled()) {
            orchestrator.startSource(saved);
        }
        return saved;
    }
}
