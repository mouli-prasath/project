package com.tripplanner.dto.trip;

import java.math.BigDecimal;
import java.time.LocalTime;

// All fields optional - only updates what's provided.
public record UpdateActivityRequest(
        LocalTime startTime,
        LocalTime endTime,
        Integer durationMinutes,
        BigDecimal estimatedCost,
        String description,
        String category
) {}
