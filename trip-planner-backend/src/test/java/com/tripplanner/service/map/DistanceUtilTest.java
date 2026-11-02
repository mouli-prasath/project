package com.tripplanner.service.map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DistanceUtilTest {

    @Test
    void sameCoordinates_returnsZeroDistance() {
        double distance = DistanceUtil.haversineKm(15.4909, 73.8278, 15.4909, 73.8278);
        assertEquals(0.0, distance, 0.001);
    }

    @Test
    void knownCityPair_matchesApproximateRealWorldDistance() {
        // New York City to Los Angeles - real great-circle distance is ~3936 km.
        // Generous 50km tolerance since we're validating the formula is
        // correct, not chasing survey-grade precision.
        double nycLat = 40.7128, nycLon = -74.0060;
        double laLat = 34.0522, laLon = -118.2437;

        double distance = DistanceUtil.haversineKm(nycLat, nycLon, laLat, laLon);

        assertEquals(3936, distance, 50);
    }

    @Test
    void distanceIsSymmetric() {
        double a = DistanceUtil.haversineKm(15.5553, 73.7517, 15.4925, 73.7738);
        double b = DistanceUtil.haversineKm(15.4925, 73.7738, 15.5553, 73.7517);
        assertEquals(a, b, 0.0001);
    }

    @Test
    void nearbyPoints_returnSmallPositiveDistance() {
        // Baga Beach and Fort Aguada, Goa - roughly 8km apart.
        double distance = DistanceUtil.haversineKm(15.5553, 73.7517, 15.4925, 73.7738);
        assertEquals(true, distance > 0 && distance < 15);
    }
}
