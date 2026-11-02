package com.tripplanner.dto.share;

import com.tripplanner.dto.trip.ItineraryDayResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

// Deliberately excludes user info (id, email, name) - this is served
// without authentication to anyone with the link, so it must not leak the
// trip owner's identity.
public record PublicTripResponse(
        String title,
        String destination,
        LocalDate startDate,
        LocalDate endDate,
        Integer travelers,
        BigDecimal budget,
        List<ItineraryDayResponse> days
) {}
