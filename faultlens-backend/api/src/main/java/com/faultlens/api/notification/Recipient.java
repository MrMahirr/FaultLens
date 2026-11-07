package com.faultlens.api.notification;

import com.faultlens.api.model.NotificationSettings;

/**
 * A user as seen by the notification channels: their address book entry plus opt-ins.
 */
public record Recipient(String username, String email, NotificationSettings settings) {
}
