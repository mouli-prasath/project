package com.tripplanner.dto.ai;

// Mirrors the exact AI response schema from the project spec's "activities" shape.
// This is what we ask Gemini to return - NOT our internal Activity entity.
public record GeneratedActivity(
        String name,
        String category,
        String description,
        String startTime,        // "HH:mm", parsed into LocalTime by the caller
        Integer durationMinutes,
        Double estimatedCost,
        Double latitude,
        Double longitude,
        String address,
        // "Why this place?" - Section 5.15 Explainable AI Recommendations.
        String reason
) {}
