package com.faultlens.api.service;

import com.faultlens.analyzer.engine.AiAnalysisEngine;
import com.faultlens.analyzer.engine.EnginePreference;
import com.faultlens.api.dto.SettingsDtos.SystemSettingsResponse;
import com.faultlens.api.dto.SettingsDtos.UpdateSystemSettingsRequest;
import com.faultlens.api.model.SystemSettings;
import com.faultlens.api.repository.SystemSettingsRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The singleton system settings row. Also the analyzer's {@link EnginePreference}: the default
 * engine chosen in Settings decides at analysis time whether the AI engine is used.
 */
@Service
@Transactional(readOnly = true)
public class SystemSettingsService implements EnginePreference {
    static final String ENGINE_AI = "ai";

    private final SystemSettingsRepository systemSettingsRepository;
    private final AiAnalysisEngine aiAnalysisEngine;

    public SystemSettingsService(SystemSettingsRepository systemSettingsRepository, AiAnalysisEngine aiAnalysisEngine) {
        this.systemSettingsRepository = systemSettingsRepository;
        this.aiAnalysisEngine = aiAnalysisEngine;
    }

    /**
     * Returns the global system settings.
     * Assumes a single record is created via Flyway migration.
     */
    public SystemSettingsResponse getSettings() {
        return toResponse(getSingletonRecord());
    }

    /**
     * Updates the global system settings.
     */
    @Transactional
    public SystemSettingsResponse updateSettings(UpdateSystemSettingsRequest request) {
        SystemSettings settings = getSingletonRecord();
        settings.setLogRetentionDays(request.logRetentionDays());
        settings.setDefaultEngine(request.defaultEngine());
        settings.setLanguage(request.language());
        settings.setUpdatedAt(Instant.now());
        return toResponse(systemSettingsRepository.save(settings));
    }

    /**
     * Internal helper for other services (e.g. LogRetentionJob) to get the retention days.
     */
    public int getLogRetentionDays() {
        return getSingletonRecord().getLogRetentionDays();
    }

    /**
     * True when the operator chose the AI engine. Read per analysis (analyses are throttled to
     * one per group per hour), so a change in Settings applies without a restart.
     */
    @Override
    public boolean preferAi() {
        return ENGINE_AI.equalsIgnoreCase(getSingletonRecord().getDefaultEngine());
    }

    private SystemSettingsResponse toResponse(SystemSettings settings) {
        return new SystemSettingsResponse(
                settings.getLogRetentionDays(),
                settings.getDefaultEngine(),
                settings.getLanguage(),
                aiAnalysisEngine.isAvailable());
    }

    private SystemSettings getSingletonRecord() {
        return systemSettingsRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("System settings record is missing. Migration failed?"));
    }
}
