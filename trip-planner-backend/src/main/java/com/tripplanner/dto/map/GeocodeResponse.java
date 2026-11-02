package com.tripplanner.dto.map;

public record GeocodeResponse(
        String formattedAddress,
        Double latitude,
        Double longitude,
        String placeId
) {}
