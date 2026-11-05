package com.faultlens.api.notification;

import java.time.Instant;

/**
 * One alarm announcement, independent of the channel that carries it.
 *
 * @param title    short headline (e-mail subject, Slack first line)
 * @param message  body text; plain text, never trusted as markup (it can contain log content)
 * @param severity severity name, or {@code null} when not known
 * @param at       when the alarm was raised
 */
public record Notification(String title, String message, String severity, Instant at) {
    public static Notification of(String title, String message) {
        return new Notification(title, message, null, Instant.now());
    }
}
