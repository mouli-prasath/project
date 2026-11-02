package com.tripplanner.dto.explore;

import com.tripplanner.entity.Destination;

public record DestinationResponse(
        Long id,
        String name,
        String country,
        String description,
        String category,
        Double latitude,
        Double longitude
) {
    public static DestinationResponse from(Destination d) {
        return new DestinationResponse(
                d.getId(), d.getName(), d.getCountry(), d.getDescription(),
                d.getCategory(), d.getLatitude(), d.getLongitude()
        );
    }
}
