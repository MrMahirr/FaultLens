package com.faultlens.api.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

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
    public record SystemSettingsResponse(
            Integer logRetentionDays,
            String defaultEngine,
            String language) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record UpdateSystemSettingsRequest(
            Integer logRetentionDays,
            String defaultEngine,
            String language) {
    }
}
