package com.tripplanner.service.weather;

import com.fasterxml.jackson.databind.JsonNode;
import com.tripplanner.dto.weather.WeatherForecast;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Optional;
import java.util.Set;

/**
 * Section 5.15 "Weather-aware itinerary". Uses OpenWeatherMap's free current
 * weather endpoint as a same-day signal (a full multi-day forecast needs a
 * paid tier on most providers, so this is scoped to "today's weather at the
 * destination" - good enough to demonstrate the adaptive-itinerary concept
 * without requiring a paid API).
 *
 * IMPORTANT: this is intentionally optional. If OPENWEATHER_API_KEY isn't
 * set, every method returns Optional.empty() instead of throwing - weather
 * awareness quietly turns itself off rather than breaking itinerary
 * generation. This matches the edge-case handling principle: an optional
 * enrichment failing should never block the core feature.
 */
@Service
public class WeatherService {

    private final RestClient restClient;
    private final String apiKey;

    private static final Set<String> INDOOR_RECOMMENDED_CONDITIONS =
            Set.of("Rain", "Thunderstorm", "Snow", "Drizzle");

    public WeatherService(RestClient aiRestClient, @Value("${app.weather.api-key}") String apiKey) {
        this.restClient = aiRestClient; // reuse the generic external-API client, no need for a third bean
        this.apiKey = apiKey;
    }

    public Optional<WeatherForecast> getForecast(double latitude, double longitude) {
        if (apiKey == null || apiKey.isBlank()) {
            return Optional.empty();
        }

        try {
            String uri = UriComponentsBuilder
                    .fromHttpUrl("https://api.openweathermap.org/data/2.5/weather")
                    .queryParam("lat", latitude)
                    .queryParam("lon", longitude)
                    .queryParam("appid", apiKey)
                    .queryParam("units", "metric")
                    .toUriString();

            JsonNode root = restClient.get().uri(uri).retrieve().body(JsonNode.class);
            if (root == null) return Optional.empty();

            JsonNode weatherNode = root.path("weather").get(0);
            if (weatherNode == null) return Optional.empty();

            String condition = weatherNode.path("main").asText("");
            String description = weatherNode.path("description").asText("");

            return Optional.of(new WeatherForecast(
                    condition,
                    description,
                    INDOOR_RECOMMENDED_CONDITIONS.contains(condition)
            ));
        } catch (Exception e) {
            // Weather is an enrichment, not a requirement - fail silently
            // and let itinerary generation proceed without it.
            return Optional.empty();
        }
    }
}
