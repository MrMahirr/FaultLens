package com.faultlens.api.notification;

import com.faultlens.api.model.AlarmChannel;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Generic webhook: a JSON POST to each opted-in user's URL (P07).
 *
 * <pre>{"event":"alarm","title":"...","message":"...","severity":"ERROR","at":"...","url":"..."}</pre>
 *
 * <p>Any role may configure a webhook for themselves, so URLs go through {@link WebhookUrls}
 * (https, public addresses only) at save time and again here; redirects are not followed.
 */
@Slf4j
@Component
public class WebhookChannel implements NotificationChannel {
    private final ObjectMapper objectMapper;
    private final boolean allowPrivateHosts;
    private final String appUrl;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    public WebhookChannel(ObjectMapper objectMapper,
                          @Value("${faultlens.notifications.webhook.allow-private-hosts:false}") boolean allowPrivateHosts,
                          @Value("${faultlens.notifications.app-url:http://localhost:3000}") String appUrl) {
        this.objectMapper = objectMapper;
        this.allowPrivateHosts = allowPrivateHosts;
        this.appUrl = appUrl;
    }

    @Override
    public AlarmChannel type() {
        return AlarmChannel.WEBHOOK;
    }

    @Override
    public boolean accepts(Recipient recipient) {
        var settings = recipient.settings();
        return settings.isWebhookEnabled() && settings.getWebhookUrl() != null && !settings.getWebhookUrl().isBlank();
    }

    @Override
    public boolean send(Notification notification, Recipient recipient) {
        return post(recipient.settings().getWebhookUrl(), notification);
    }

    public boolean isAllowed(String url) {
        return url != null && WebhookUrls.isAllowed(url, allowPrivateHosts);
    }

    /** One POST; false when the URL is not allowed, unreachable or answers non-2xx. */
    public boolean post(String url, Notification notification) {
        if (!isAllowed(url)) {
            log.warn("Refusing to call a webhook that is not a public https URL");
            return false;
        }
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("event", "alarm");
            payload.put("title", notification.title());
            payload.put("message", notification.message());
            payload.put("severity", notification.severity());
            payload.put("at", notification.at().toString());
            payload.put("url", appUrl + "/alarms");
            HttpRequest request = HttpRequest.newBuilder(URI.create(url.trim()))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "FaultLens-Webhook/1")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();
            int status = httpClient.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
            if (status >= 200 && status < 300) {
                return true;
            }
            log.warn("Webhook answered HTTP {}", status);
            return false;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception ex) {
            log.warn("Webhook call failed: {}", ex.getMessage());
            return false;
        }
    }
}
