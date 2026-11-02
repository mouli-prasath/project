package com.tripplanner.dto.hotel;

import java.util.List;

public record HotelResponse(
        String placeId,
        String name,
        String address,
        Double rating,               // OSM self-declared "stars" tag if present, NOT a review score - see HotelCandidate
        Integer userRatingCount,     // always null under OSM - no review system
        String priceLevel,           // always null under OSM - no pricing data
        String imageUrl,             // always null under OSM - no photo data
        Double latitude,
        Double longitude,
        List<String> amenities,
        String detailsUrl,
        double distanceFromItineraryCenterKm,
        String nearestActivityName,
        Double nearestActivityDistanceKm,
        // Null (not false!) when we can't determine budget fit - which is
        // currently ALWAYS true under OSM, since priceLevel is never
        // populated. false would misleadingly imply "confirmed over
        // budget"; null honestly means "unknown, provider has no pricing".
        Boolean withinSuggestedBudget
) {}
