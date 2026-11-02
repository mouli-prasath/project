package com.tripplanner.dto.trip;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalTime;

// Used for POST /api/trips/{id}/activities - a user manually adding a stop
// the AI didn't suggest. Requires real coordinates (typically resolved via
// GET /api/map/geocode or /api/map/places/search on the frontend first).
public record CreateActivityRequest(
        @NotNull Integer dayNumber,
        @NotBlank String placeName,
        String address,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
        LocalTime startTime,
        LocalTime endTime,
        Integer durationMinutes,
        @DecimalMin("0.0") BigDecimal estimatedCost,
        String description,
        String category
) {}
