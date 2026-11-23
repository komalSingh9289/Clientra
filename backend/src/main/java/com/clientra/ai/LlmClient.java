package com.clientra.ai;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Thin client for the external LLM (synopsis 6.2: "OpenAI GPT-4 API / Gemini API").
 * Provider is chosen with clientra.ai.provider = none | openai | gemini.
 *  - "none" (default) or a missing key: complete() returns empty and AiService uses the built-in
 *    rule-based engine, so the product keeps working without any external service.
 *  - Any network/API failure also returns empty (graceful fallback, synopsis section 12 limitation 2).
 * PRIVACY: when enabled, the lead's name, company and interaction notes are sent to the provider.
 */
@Component
public class LlmClient {
    private static final Logger log = LoggerFactory.getLogger(LlmClient.class);

    private final String provider;
    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final RestClient http;

    public LlmClient(@Value("${clientra.ai.provider:none}") String provider,
                     @Value("${clientra.ai.api-key:}") String apiKey,
                     @Value("${clientra.ai.model:gpt-4o-mini}") String model,
                     @Value("${clientra.ai.base-url:https://api.openai.com/v1}") String baseUrl) {
        this.provider = provider == null ? "none" : provider.trim().toLowerCase();
        this.apiKey = apiKey;
        this.model = model;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(10_000);
        f.setReadTimeout(25_000);
        this.http = RestClient.builder().requestFactory(f).build();
    }

    public boolean enabled() {
        return !"none".equals(provider) && apiKey != null && !apiKey.isBlank();
    }

    /** Name written to the AI log so you can see which engine produced a result. */
    public String engineName() { return enabled() ? provider + ":" + model : "rule-based"; }

    /** Returns the model's text answer, or empty if disabled / failed. */
    public Optional<String> complete(String system, String user) {
        if (!enabled()) return Optional.empty();
        try {
            String text = "gemini".equals(provider) ? callGemini(system, user) : callOpenAi(system, user);
            return (text == null || text.isBlank()) ? Optional.empty() : Optional.of(text);
        } catch (Exception e) {
            log.warn("LLM call failed, falling back to the rule-based engine: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /** OpenAI-compatible "chat completions" endpoint. */
    private String callOpenAi(String system, String user) {
        Map<String, Object> body = Map.of(
                "model", model,
                "temperature", 0.4,
                "messages", List.of(Map.of("role", "system", "content", system),
                                    Map.of("role", "user", "content", user)));
        JsonNode res = http.post().uri(baseUrl + "/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body).retrieve().body(JsonNode.class);
        return res == null ? null : res.path("choices").path(0).path("message").path("content").asText(null);
    }

    /** Google Gemini generateContent endpoint (base-url is ignored for this provider). */
    private String callGemini(String system, String user) {
        Map<String, Object> body = Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", system))),
                "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", user)))));
        JsonNode res = http.post()
                .uri("https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent")
                .header("x-goog-api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body).retrieve().body(JsonNode.class);
        return res == null ? null : res.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText(null);
    }
}
