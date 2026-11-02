package com.tripplanner.dto.trip;

import com.tripplanner.entity.TravelStyle;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

// Matches the request shape defined in the project spec exactly:
// { source, destination, startDate, endDate, travelers, budget, travelStyle, interests, additionalPreferences }
public record CreateTripRequest(

        @NotBlank(message = "Source is required")
        String source,

        @NotBlank(message = "Destination is required")
        String destination,

        @NotNull(message = "Start date is required")
        @FutureOrPresent(message = "Start date cannot be in the past")
        LocalDate startDate,

        @NotNull(message = "End date is required")
        LocalDate endDate,

        @NotNull(message = "Number of travelers is required")
        @Min(value = 1, message = "There must be at least 1 traveler")
        @Max(value = 50, message = "Number of travelers seems unrealistic")
        Integer travelers,

        @NotNull(message = "Budget is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Budget must be greater than 0")
        BigDecimal budget,

        @NotNull(message = "Travel style is required")
        TravelStyle travelStyle,

        List<String> interests,

        String additionalPreferences,

        // Optional fields from the "important details" addendum - safe to omit.
        String preferredTransport,
        String accommodationPreference,

        String title
) {}
