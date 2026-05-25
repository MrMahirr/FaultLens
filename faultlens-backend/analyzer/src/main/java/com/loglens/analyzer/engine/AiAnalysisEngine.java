package com.loglens.analyzer.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loglens.common.dto.LogEntryDto;
import com.loglens.processor.model.LogGroup;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "loglens.analyzer.ai", name = "enabled", havingValue = "true")
public class AiAnalysisEngine implements AnalysisEngine {
    private final String apiKey;
    private final String model;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public AiAnalysisEngine(
            @Value("${loglens.analyzer.ai.openai-api-key:}") String apiKey,
            @Value("${loglens.analyzer.ai.model:gpt-4o-mini}") String model,
            ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.model = model;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Sends the log context to OpenAI and parses a JSON analysis response.
     */
    @Override
    public AnalysisResult analyze(LogEntryDto entry, LogGroup group) {
        if (!isAvailable()) {
            return AnalysisResult.unknown();
        }
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "model", model,
                    "temperature", 0.1,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt()),
                            Map.of("role", "user", "content", userPrompt(entry, group))
                    )));
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.openai.com/v1/chat/completions"))
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return parseResponse(response.body());
            }
            log.warn("AI analysis failed with status {}", response.statusCode());
        } catch (Exception ex) {
            log.warn("AI analysis request failed", ex);
        }
        return AnalysisResult.unknown();
    }

    /**
     * Returns whether AI analysis is configured.
     */
    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isBlank();
    }

    private AnalysisResult parseResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        String content = root.path("choices").path(0).path("message").path("content").asText("{}");
        JsonNode result = objectMapper.readTree(stripJsonFence(content));
        return new AnalysisResult(
                result.path("rootCause").asText("Bilinmeyen hata"),
                result.path("suggestion").asText("Log detaylarini incele."),
                null,
                result.path("confidenceScore").asDouble(0.1),
                "AI",
                "AiAnalysisEngine"
        );
    }

    private String stripJsonFence(String content) {
        String trimmed = content == null ? "{}" : content.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```(?:json)?", "").replaceFirst("```$", "").trim();
        }
        return trimmed;
    }

    private String systemPrompt() {
        return "Sen bir backend sistem uzmanisin. Verilen Java/sistem log hatasinin kok nedenini ve cozum onerisini JSON formatinda dondur: {rootCause, suggestion, confidenceScore}";
    }

    private String userPrompt(LogEntryDto entry, LogGroup group) {
        String message = entry.getMessage() == null ? "" : entry.getMessage();
        String stackTrace = entry.getStackTrace() == null ? "" : entry.getStackTrace();
        String groupMessage = group == null || group.getFirstMessage() == null ? "" : group.getFirstMessage();
        String prompt = "message:\n%s\n\nstackTrace:\n%s\n\ngroupFirstMessage:\n%s".formatted(message, stackTrace, groupMessage);
        return prompt.substring(0, Math.min(prompt.length(), 6000));
    }
}
