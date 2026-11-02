package com.tripplanner.dto.map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record Waypoint(
        @NotBlank(message = "Waypoint name is required")
        String name,

        @NotNull(message = "Waypoint latitude is required")
        Double latitude,

        @NotNull(message = "Waypoint longitude is required")
        Double longitude
) {}
