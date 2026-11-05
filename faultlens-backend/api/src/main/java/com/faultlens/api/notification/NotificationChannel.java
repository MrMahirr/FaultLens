package com.faultlens.api.notification;

import com.faultlens.api.model.AlarmChannel;

/**
 * A way to announce alarms (P07). {@link com.faultlens.api.service.AlarmNotifier} picks the
 * channels an alarm rule asked for and fans out:
 *
 * <ul>
 *   <li>broadcast channels (in-app) are sent once, with {@code recipient == null};</li>
 *   <li>personal channels (Slack, e-mail, webhook) are sent to every recipient they
 *       {@link #accepts accept}, i.e. users who opted in and configured an address.</li>
 * </ul>
 *
 * <p>{@link #send} returns {@code false} (or throws) on failure; the notifier retries a few times.
 * A channel must not let one recipient's failure affect the others.
 */
public interface NotificationChannel {
    AlarmChannel type();

    /** Whether the server is set up for this channel at all (e.g. SMTP configured). */
    default boolean isAvailable() {
        return true;
    }

    /** Sent once per alarm rather than per recipient. */
    default boolean isBroadcast() {
        return false;
    }

    /** Whether this recipient opted in and has an address for this channel. */
    boolean accepts(Recipient recipient);

    boolean send(Notification notification, Recipient recipient);
}
