package com.tripplanner.dto.trip;

import java.util.List;

public record ItineraryResponse(
        Long tripId,
        String tripTitle,
        String summary,
        List<ItineraryDayResponse> days
) {}
