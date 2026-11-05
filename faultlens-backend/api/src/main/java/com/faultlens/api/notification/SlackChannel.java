package com.faultlens.api.notification;

import com.faultlens.api.model.AlarmChannel;
import com.faultlens.api.service.SlackNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Each opted-in user's Slack incoming webhook. */
@Component
@RequiredArgsConstructor
public class SlackChannel implements NotificationChannel {
    private final SlackNotificationService slack;

    @Override
    public AlarmChannel type() {
        return AlarmChannel.SLACK;
    }

    @Override
    public boolean accepts(Recipient recipient) {
        var settings = recipient.settings();
        return settings.isSlackEnabled() && settings.getSlackWebhook() != null
                && !settings.getSlackWebhook().isBlank();
    }

    @Override
    public boolean send(Notification notification, Recipient recipient) {
        return slack.sendNotification(recipient.settings().getSlackWebhook(),
                notification.title() + "\n" + notification.message());
    }
}
