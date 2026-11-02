package com.tripplanner.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.dto.ai.GeneratedItinerary;
import com.tripplanner.exception.ExternalServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * The ONLY class that talks to the Gemini API directly. Mirrors the same
 * isolation pattern as OsmMapsClient - callers work with GeneratedItinerary,
 * never raw JSON or HTTP details.
 */
@Service
public class GeminiClient {

    private static final String URL_TEMPLATE =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public GeminiClient(
            RestClient aiRestClient,
            ObjectMapper objectMapper,
            @Value("${app.ai.gemini-api-key}") String apiKey,
            @Value("${app.ai.gemini-model}") String model
    ) {
        this.restClient = aiRestClient;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
    }

    public GeneratedItinerary generateItinerary(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ExternalServiceException(
                    "Gemini API key is not configured on the server (GEMINI_API_KEY)");
        }

        String url = String.format(URL_TEMPLATE, model, apiKey);

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "temperature", 0.7
                )
        );

        JsonNode root;
        try {
            root = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (Exception e) {
            throw new ExternalServiceException("Failed to reach Gemini API: " + e.getMessage(), e);
        }

        if (root == null || !root.path("candidates").isArray() || root.path("candidates").isEmpty()) {
            // Often means the prompt was blocked by safety filters - check
            // promptFeedback.blockReason in the raw response if this happens a lot.
            throw new ExternalServiceException("Gemini returned no candidates for this request");
        }

        String text = root.path("candidates").get(0)
                .path("content").path("parts").get(0)
                .path("text").asText("");

        // Defensive: even with responseMimeType=application/json, strip
        // markdown fences in case the model adds them anyway.
        String cleaned = text.replaceAll("```json|```", "").trim();

        try {
            return objectMapper.readValue(cleaned, GeneratedItinerary.class);
        } catch (Exception e) {
            throw new ExternalServiceException(
                    "Gemini did not return valid itinerary JSON: " + e.getMessage());
        }
    }
}
