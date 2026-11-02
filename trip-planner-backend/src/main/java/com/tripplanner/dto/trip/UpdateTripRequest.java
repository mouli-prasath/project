package com.tripplanner.dto.trip;

import com.tripplanner.entity.TravelStyle;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

// All fields optional - PUT/PATCH only updates what's provided.
public record UpdateTripRequest(
        String title,
        LocalDate startDate,
        LocalDate endDate,
        Integer travelers,
        BigDecimal budget,
        TravelStyle travelStyle,
        List<String> interests,
        String additionalPreferences,
        String preferredTransport,
        String accommodationPreference
) {}
