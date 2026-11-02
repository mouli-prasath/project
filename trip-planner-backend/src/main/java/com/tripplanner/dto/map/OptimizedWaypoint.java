package com.tripplanner.dto.map;

public record OptimizedWaypoint(
        int sequence,
        String name,
        Double latitude,
        Double longitude,
        // Distance in km travelled from the PREVIOUS stop to reach this one
        // (from the trip's starting point, for the first stop).
        double legDistanceKm
) {}
