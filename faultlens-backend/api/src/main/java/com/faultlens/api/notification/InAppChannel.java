package com.faultlens.api.notification;

import com.faultlens.api.model.AlarmChannel;
import com.faultlens.api.service.InAppNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** WebSocket broadcast to every open dashboard. */
@Component
@RequiredArgsConstructor
public class InAppChannel implements NotificationChannel {
    private final InAppNotificationService inApp;

    @Override
    public AlarmChannel type() {
        return AlarmChannel.IN_APP;
    }

    @Override
    public boolean isBroadcast() {
        return true;
    }

    @Override
    public boolean accepts(Recipient recipient) {
        return true;
    }

    @Override
    public boolean send(Notification notification, Recipient recipient) {
        inApp.broadcastAlarm(notification.title(), notification.message());
        return true;
    }
}
