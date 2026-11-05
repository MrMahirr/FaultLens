package com.faultlens.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

public final class SettingsDtos {
    private SettingsDtos() {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record NotificationSettingsResponse(
            boolean emailEnabled,
            boolean slackEnabled,
            String slackWebhook,
            boolean pushEnabled,
            boolean mobileEnabled,
            String emailjsServiceId,
            String emailjsTemplateId,
            String emailjsPublicKey) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record UpdateNotificationRequest(
            boolean emailEnabled,
            boolean slackEnabled,
            String slackWebhook,
            boolean pushEnabled,
            boolean mobileEnabled,
            String emailjsServiceId,
            String emailjsTemplateId,
            String emailjsPublicKey) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    /**
     * {@code aiAvailable}: whether the AI engine can run at all (AI_ENABLED and an API key). When
     * false, "ai" as default engine falls back to the rule-based engine.
     */
    public record SystemSettingsResponse(
            Integer logRetentionDays,
            String defaultEngine,
            String language,
            boolean aiAvailable) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record UpdateSystemSettingsRequest(
            @NotNull @Min(0) @Max(3650) Integer logRetentionDays,
            @NotNull @Pattern(regexp = "ai|rule", message = "must be 'ai' or 'rule'") String defaultEngine,
            @NotNull @Pattern(regexp = "tr|en", message = "must be 'tr' or 'en'") String language) {
    }
}
