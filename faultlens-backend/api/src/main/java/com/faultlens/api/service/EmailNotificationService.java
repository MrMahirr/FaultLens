package com.faultlens.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailNotificationService {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public EmailNotificationService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    /**
     * Sends an email to the specified address using Email.js API.
     */
    public boolean sendEmail(String toEmail, String subject, String body, String serviceId, String templateId, String publicKey) {
        if (toEmail == null || toEmail.isBlank() || serviceId == null || templateId == null || publicKey == null) {
            log.warn("Recipient email or EmailJS credentials are empty, skipping email notification.");
            return false;
        }

        try {
            Map<String, Object> payload = Map.of(
                "service_id", serviceId,
                "template_id", templateId,
                "user_id", publicKey,
                "template_params", Map.of(
                    "to_email", toEmail,
                    "subject", subject,
                    "message", body
                )
            );
            
            String jsonPayload = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.emailjs.com/api/v1.0/email/send"))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("Successfully sent EmailJS notification to {}", toEmail);
                return true;
            } else {
                log.error("Failed to send EmailJS notification. Status: {}, Body: {}", response.statusCode(), response.body());
                return false;
            }
        } catch (Exception e) {
            log.error("Failed to send email notification to {}", toEmail, e);
            return false;
        }
    }
}
