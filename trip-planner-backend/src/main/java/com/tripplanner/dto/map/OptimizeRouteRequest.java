package com.tripplanner.dto.map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record OptimizeRouteRequest(

        @NotNull(message = "A starting point is required (e.g. the hotel)")
        @Valid
        Waypoint start,

        @NotEmpty(message = "At least one waypoint is required to optimize a route")
        @Size(max = 25, message = "Cannot optimize more than 25 waypoints at once")
        @Valid
        List<Waypoint> waypoints
) {}
