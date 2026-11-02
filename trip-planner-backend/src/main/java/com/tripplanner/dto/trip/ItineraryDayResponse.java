package com.tripplanner.dto.trip;

import com.tripplanner.entity.ItineraryDay;

import java.time.LocalDate;
import java.util.List;

public record ItineraryDayResponse(
        Long id,
        Integer dayNumber,
        LocalDate date,
        String theme,
        List<ActivityResponse> activities
) {
    public static ItineraryDayResponse from(ItineraryDay day) {
        return new ItineraryDayResponse(
                day.getId(),
                day.getDayNumber(),
                day.getDate(),
                day.getTheme(),
                day.getActivities().stream().map(ActivityResponse::from).toList()
        );
    }
}
