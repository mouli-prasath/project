package com.tripplanner.dto.map;

import java.util.List;

public record OptimizeRouteResponse(
        List<OptimizedWaypoint> route,
        double totalDistanceKm
) {}
