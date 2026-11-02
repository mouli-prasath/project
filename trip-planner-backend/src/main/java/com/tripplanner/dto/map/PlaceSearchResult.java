package com.tripplanner.dto.map;

public record PlaceSearchResult(
        String placeId,
        String name,
        String address,
        String category,
        Double rating,
        Double latitude,
        Double longitude,
        // Best-effort daily opening/closing time in "HH:mm", nullable if
        // Google doesn't report hours for this place OR hours vary by day
        // (we take the first period found - a known simplification, see
        // README "Known limitations").
        String openingTime,
        String closingTime
) {}
