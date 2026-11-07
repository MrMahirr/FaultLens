package com.faultlens.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

public final class SettingsDtos {
    private SettingsDtos() {
    }

    /**
     * The caller's notification preferences. {@code email} is the profile address alarm e-mails
     * go to; {@code emailAvailable} says whether the server has SMTP configured at all.
     */
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record NotificationSettingsResponse(
            boolean emailEnabled,
            boolean slackEnabled,
            String slackWebhook,
            boolean webhookEnabled,
            String webhookUrl,
            boolean pushEnabled,
            boolean mobileEnabled,
            String email,
            boolean emailAvailable) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record UpdateNotificationRequest(
            boolean emailEnabled,
            boolean slackEnabled,
            @Size(max = 500) String slackWebhook,
            boolean webhookEnabled,
            @Size(max = 500) String webhookUrl,
            boolean pushEnabled,
            boolean mobileEnabled) {
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
