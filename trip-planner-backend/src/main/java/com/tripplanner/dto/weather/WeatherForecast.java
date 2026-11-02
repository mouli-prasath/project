package com.tripplanner.dto.weather;

public record WeatherForecast(
        String condition,       // e.g. "Rain", "Clear", "Clouds"
        String description,     // e.g. "moderate rain"
        boolean recommendIndoor // true for rain/storm/snow conditions
) {}
