package com.faultlens.api.notification;

import com.faultlens.api.model.AlarmChannel;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

/**
 * Alarm e-mails over SMTP ({@code spring.mail.*}, P07; replaced EmailJS, whose keys lived in the
 * browser). Sent to the user's profile address when they enabled e-mail notifications.
 *
 * <p>Multipart: a plain-text part and a small HTML template. Title and message may contain log
 * content, so both are HTML-escaped and never treated as markup.
 */
@Slf4j
@Component
public class EmailChannel implements NotificationChannel {
    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z").withZone(ZoneId.systemDefault());

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String host;
    private final String from;
    private final String appUrl;

    public EmailChannel(ObjectProvider<JavaMailSender> mailSender,
                        @Value("${spring.mail.host:}") String host,
                        @Value("${faultlens.notifications.mail-from:FaultLens <faultlens@localhost>}") String from,
                        @Value("${faultlens.notifications.app-url:http://localhost:3000}") String appUrl) {
        this.mailSender = mailSender;
        this.host = host;
        this.from = from;
        this.appUrl = appUrl;
    }

    @Override
    public AlarmChannel type() {
        return AlarmChannel.EMAIL;
    }

    @Override
    public boolean isAvailable() {
        return host != null && !host.isBlank() && mailSender.getIfAvailable() != null;
    }

    @Override
    public boolean accepts(Recipient recipient) {
        return recipient.settings().isEmailEnabled() && isAddress(recipient.email());
    }

    @Override
    public boolean send(Notification notification, Recipient recipient) {
        return sendTo(recipient.email(), notification);
    }

    /** Sends to one address; false when SMTP is not configured or delivery failed. */
    public boolean sendTo(String address, Notification notification) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (!isAvailable() || sender == null) {
            log.warn("E-mail notification skipped: SMTP is not configured (SMTP_HOST)");
            return false;
        }
        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(address);
            helper.setSubject("[FaultLens] " + oneLine(notification.title()));
            helper.setText(plainText(notification), html(notification));
            sender.send(message);
            return true;
        } catch (Exception ex) {
            log.warn("E-mail notification to {} failed: {}", address, ex.getMessage());
            return false;
        }
    }

    static boolean isAddress(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        try {
            new InternetAddress(email, true);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    /** Subjects are a single header line: no CR/LF from log content. */
    private static String oneLine(String text) {
        return text == null ? "" : text.replaceAll("[\\r\\n]+", " ").strip();
    }

    String plainText(Notification n) {
        StringBuilder text = new StringBuilder(oneLine(n.title())).append("\n\n");
        text.append(n.message() == null ? "" : n.message()).append("\n\n");
        if (n.severity() != null) {
            text.append("Seviye: ").append(n.severity()).append('\n');
        }
        text.append("Zaman: ").append(TIME.format(n.at())).append('\n');
        text.append(appUrl).append("/alarms\n");
        return text.toString();
    }

    String html(Notification n) {
        String title = HtmlUtils.htmlEscape(oneLine(n.title()));
        String message = HtmlUtils.htmlEscape(n.message() == null ? "" : n.message());
        String severity = n.severity() == null ? "" : HtmlUtils.htmlEscape(n.severity());
        String link = HtmlUtils.htmlEscape(appUrl + "/alarms");
        return """
                <!DOCTYPE html>
                <html lang="tr"><body style="margin:0;padding:24px;background:#f4f5f7;font-family:Segoe UI,Arial,sans-serif;color:#1f2937">
                <table role="presentation" width="100%%" style="max-width:600px;margin:0 auto;background:#ffffff;border-radius:12px;border:1px solid #e5e7eb">
                <tr><td style="padding:20px 24px;border-bottom:1px solid #e5e7eb">
                  <div style="font-size:12px;letter-spacing:.08em;color:#6b7280;text-transform:uppercase">FaultLens alarmı %s</div>
                  <h1 style="margin:6px 0 0;font-size:18px">%s</h1>
                </td></tr>
                <tr><td style="padding:20px 24px">
                  <pre style="margin:0;white-space:pre-wrap;word-break:break-word;font-family:Consolas,monospace;font-size:13px;background:#f9fafb;border-radius:8px;padding:12px">%s</pre>
                  <p style="margin:16px 0 0;font-size:12px;color:#6b7280">%s</p>
                  <p style="margin:20px 0 0"><a href="%s" style="background:#3b82f6;color:#ffffff;text-decoration:none;padding:10px 16px;border-radius:8px;font-size:14px">Alarmları aç</a></p>
                </td></tr>
                </table>
                </body></html>
                """.formatted(severity.isEmpty() ? "" : "· " + severity, title, message,
                HtmlUtils.htmlEscape(TIME.format(n.at())), link);
    }
}
