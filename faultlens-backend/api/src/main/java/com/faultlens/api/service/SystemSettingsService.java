package com.faultlens.api.service;

import com.faultlens.api.dto.SettingsDtos.SystemSettingsResponse;
import com.faultlens.api.dto.SettingsDtos.UpdateSystemSettingsRequest;
import com.faultlens.api.model.SystemSettings;
import com.faultlens.api.repository.SystemSettingsRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SystemSettingsService {

    private final SystemSettingsRepository systemSettingsRepository;

    /**
     * Returns the global system settings.
     * Assumes a single record is created via Flyway migration.
     */
    public SystemSettingsResponse getSettings() {
        SystemSettings settings = getSingletonRecord();
        return new SystemSettingsResponse(
                settings.getLogRetentionDays(),
                settings.getDefaultEngine(),
                settings.getLanguage()
        );
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
        
        SystemSettings saved = systemSettingsRepository.save(settings);
        return new SystemSettingsResponse(
                saved.getLogRetentionDays(),
                saved.getDefaultEngine(),
                saved.getLanguage()
        );
    }

    /**
     * Internal helper for other services (e.g. LogRetentionJob) to get the retention days.
     */
    public int getLogRetentionDays() {
        return getSingletonRecord().getLogRetentionDays();
    }

    private SystemSettings getSingletonRecord() {
        return systemSettingsRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("System settings record is missing. Migration failed?"));
    }
}
