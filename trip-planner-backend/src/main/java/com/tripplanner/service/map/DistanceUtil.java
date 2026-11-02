package com.tripplanner.service.map;

/**
 * Great-circle distance between two lat/lng points using the Haversine
 * formula. This is intentionally NOT an API call - it's a fast, free,
 * deterministic approximation good enough for ordering nearby stops within a
 * single day's itinerary. A routing API (e.g. Google Directions) would give
 * actual road/travel-time distance and is a reasonable v2 upgrade, but
 * Haversine is the right first implementation: it costs nothing, has no
 * rate limit, and is what the project doc calls a "TSP-inspired" approach
 * doesn't require perfect road-accurate distances to be useful.
 */
public final class DistanceUtil {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private DistanceUtil() {
        // utility class
    }

    public static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }
}
