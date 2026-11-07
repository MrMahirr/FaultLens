package com.faultlens.api.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.faultlens.api.model.NotificationSettings;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeMessage;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

class EmailChannelTest {
    private JavaMailSender sender;
    private ObjectProvider<JavaMailSender> provider;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        sender = mock(JavaMailSender.class);
        // Real MIME messages (no SMTP connection is made).
        when(sender.createMimeMessage()).thenAnswer(inv -> new JavaMailSenderImpl().createMimeMessage());
        provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(sender);
    }

    private EmailChannel channel(String host) {
        return new EmailChannel(provider, host, "FaultLens <alerts@example.com>", "https://faultlens.example.com");
    }

    private static Recipient recipient(String email, boolean enabled) {
        NotificationSettings settings = new NotificationSettings();
        settings.setEmailEnabled(enabled);
        return new Recipient("ayse", email, settings);
    }

    @Test
    void isOffWithoutAnSmtpHost() {
        EmailChannel channel = channel("");

        assertThat(channel.isAvailable()).isFalse();
        assertThat(channel.sendTo("ops@example.com", Notification.of("t", "m"))).isFalse();
        verify(sender, never()).send(any(MimeMessage.class));
    }

    @Test
    void acceptsOnlyOptedInUsersWithAValidAddress() {
        EmailChannel channel = channel("smtp.example.com");

        assertThat(channel.accepts(recipient("ops@example.com", true))).isTrue();
        assertThat(channel.accepts(recipient("ops@example.com", false))).isFalse();
        assertThat(channel.accepts(recipient(null, true))).isFalse();
        assertThat(channel.accepts(recipient("not an address", true))).isFalse();
    }

    @Test
    void sendsAMultipartMailWithEscapedLogContentAndASingleLineSubject() throws Exception {
        EmailChannel channel = channel("smtp.example.com");
        Notification alarm = new Notification("Alarm: <b>x</b>\r\nBcc: victim@example.com",
                "NullPointerException at <script>alert(1)</script>", "ERROR", Instant.parse("2026-11-05T10:00:00Z"));

        assertThat(channel.send(alarm, recipient("ops@example.com", true))).isTrue();

        ArgumentCaptor<MimeMessage> sent = ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender).send(sent.capture());
        MimeMessage message = sent.getValue();
        message.saveChanges(); // done by the transport on a real send: sets the MIME headers
        assertThat(message.getAllRecipients()).extracting(Object::toString).containsExactly("ops@example.com");
        assertThat(message.getSubject()).isEqualTo("[FaultLens] Alarm: <b>x</b> Bcc: victim@example.com");
        assertThat(message.getHeader("Bcc")).isNull();

        String html = findPart(message, "text/html");
        assertThat(html).contains("&lt;script&gt;alert(1)&lt;/script&gt;").doesNotContain("<script>");
        assertThat(html).contains("https://faultlens.example.com/alarms").contains("ERROR");
        assertThat(findPart(message, "text/plain")).contains("<script>alert(1)</script>");
    }

    @Test
    void reportsDeliveryFailures() {
        doThrow(new MailSendException("connection refused")).when(sender).send(any(MimeMessage.class));

        assertThat(channel("smtp.example.com").sendTo("ops@example.com", Notification.of("t", "m"))).isFalse();
    }

    private static String findPart(Part part, String type) throws Exception {
        if (part.isMimeType(type)) {
            return (String) part.getContent();
        }
        if (part.getContent() instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                String found = findPart(multipart.getBodyPart(i), type);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
