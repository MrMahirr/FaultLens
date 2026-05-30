package com.faultlens.api.controller;

import com.faultlens.api.dto.SettingsDtos.NotificationSettingsResponse;
import com.faultlens.api.dto.SettingsDtos.SystemSettingsResponse;
import com.faultlens.api.dto.SettingsDtos.UpdateNotificationRequest;
import com.faultlens.api.dto.SettingsDtos.UpdateSystemSettingsRequest;
import com.faultlens.api.service.NotificationSettingsService;
import com.faultlens.api.service.SystemSettingsService;
import com.faultlens.common.dto.ApiResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final NotificationSettingsService notificationSettingsService;
    private final SystemSettingsService systemSettingsService;

    /**
     * Returns notification preferences for the authenticated user.
     */
    @GetMapping("/notifications")
    public ResponseEntity<ApiResponse<NotificationSettingsResponse>> getNotifications(Principal principal) {
        return ResponseEntity.ok(ApiResponse.ok(notificationSettingsService.getByUsername(principal.getName())));
    }

    /**
     * Updates notification preferences for the authenticated user.
     */
    @PutMapping("/notifications")
    public ResponseEntity<ApiResponse<NotificationSettingsResponse>> updateNotifications(
            Principal principal,
            @Valid @RequestBody UpdateNotificationRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(notificationSettingsService.update(principal.getName(), request)));
    }

    /**
     * Returns the global system settings.
     */
    @GetMapping("/system")
    public ResponseEntity<ApiResponse<SystemSettingsResponse>> getSystemSettings() {
        return ResponseEntity.ok(ApiResponse.ok(systemSettingsService.getSettings()));
    }

    /**
     * Updates the global system settings.
     */
    @PutMapping("/system")
    public ResponseEntity<ApiResponse<SystemSettingsResponse>> updateSystemSettings(
            @Valid @RequestBody UpdateSystemSettingsRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(systemSettingsService.updateSettings(request)));
    }
}
