package com.faultlens.api.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@Service
public class SlackNotificationService {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public SlackNotificationService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                // A redirect could bounce the request to an internal address after validation.
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    /**
     * Sends a message to the specified Slack Webhook URL.
     *
     * @param webhookUrl The Slack Webhook URL
     * @param message    The message to send
     * @return true if successful, false otherwise
     */
    public boolean sendNotification(String webhookUrl, String message) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.warn("Slack webhook URL is empty, skipping notification.");
            return false;
        }
        // Checked at send time as well, so webhooks stored before validation existed are covered.
        if (!SlackWebhookUrls.isAllowed(webhookUrl)) {
            log.warn("Refusing to send Slack notification to a non-Slack URL.");
            return false;
        }

        try {
            String payload = objectMapper.writeValueAsString(Map.of("text", message));
            
            HttpRequest request = HttpRequest.newBuilder(URI.create(webhookUrl))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
                    
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("Successfully sent Slack notification.");
                return true;
            } else {
                log.error("Failed to send Slack notification. Status: {}", response.statusCode());
                return false;
            }
        } catch (Exception e) {
            log.error("Exception while sending Slack notification", e);
            return false;
        }
    }
}
