package com.tripplanner.dto.hotel;

import java.util.List;

public record HotelSearchResponse(
        Long tripId,
        double centerLatitude,
        double centerLongitude,
        int radiusKm,
        String provider,
        String dataDisclaimer,
        String suggestedMaxPriceLevel,        // nullable if budget data isn't available yet
        Double remainingBudgetPerNight,        // nullable for the same reason - real number, not fabricated
        List<HotelResponse> hotels
) {}
