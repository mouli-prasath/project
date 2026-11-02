package com.tripplanner.dto.trip;

import com.tripplanner.entity.Trip;
import com.tripplanner.entity.TravelStyle;
import com.tripplanner.entity.TripStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

public record TripResponse(
        Long id,
        String title,
        String source,
        String destination,
        LocalDate startDate,
        LocalDate endDate,
        Integer travelers,
        BigDecimal budget,
        TravelStyle travelStyle,
        TripStatus status,
        List<String> interests,
        String additionalPreferences,
        String preferredTransport,
        String accommodationPreference,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static TripResponse from(Trip trip, String interestsCsv, String additionalPreferences) {
        List<String> interests = (interestsCsv == null || interestsCsv.isBlank())
                ? List.of()
                : Arrays.asList(interestsCsv.split(","));

        return new TripResponse(
                trip.getId(),
                trip.getTitle(),
                trip.getSource(),
                trip.getDestination(),
                trip.getStartDate(),
                trip.getEndDate(),
                trip.getTravelers(),
                trip.getBudget(),
                trip.getTravelStyle(),
                trip.getStatus(),
                interests,
                additionalPreferences,
                trip.getPreferredTransport(),
                trip.getAccommodationPreference(),
                trip.getCreatedAt(),
                trip.getUpdatedAt()
        );
    }
}
