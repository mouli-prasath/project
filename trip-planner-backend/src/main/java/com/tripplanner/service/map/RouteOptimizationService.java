package com.tripplanner.service.map;

import com.tripplanner.dto.map.OptimizeRouteRequest;
import com.tripplanner.dto.map.OptimizeRouteResponse;
import com.tripplanner.dto.map.OptimizedWaypoint;
import com.tripplanner.dto.map.Waypoint;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Reorders a list of waypoints (e.g. the places an AI suggested for a day)
 * into a geographically efficient visiting sequence, starting from a fixed
 * point (typically the hotel).
 *
 * Algorithm: nearest-neighbor greedy - at each step, go to whichever
 * remaining waypoint is closest to the current position. This is O(n^2),
 * which is completely fine for itinerary-sized inputs (a handful of stops
 * per day, capped at 25 per request). It does not guarantee the
 * mathematically optimal route (true TSP is NP-hard) but it reliably
 * eliminates the obviously-wasteful backtracking an LLM's suggested
 * ordering can produce, which is the actual problem worth solving here.
 */
@Service
public class RouteOptimizationService {

    public OptimizeRouteResponse optimize(OptimizeRouteRequest request) {
        List<Waypoint> remaining = new ArrayList<>(request.waypoints());
        List<OptimizedWaypoint> ordered = new ArrayList<>(remaining.size());

        double currentLat = request.start().latitude();
        double currentLon = request.start().longitude();
        double totalDistance = 0.0;
        int sequence = 1;

        while (!remaining.isEmpty()) {
            Waypoint nearest = null;
            double nearestDistance = Double.MAX_VALUE;

            for (Waypoint candidate : remaining) {
                double distance = DistanceUtil.haversineKm(
                        currentLat, currentLon, candidate.latitude(), candidate.longitude());
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearest = candidate;
                }
            }

            totalDistance += nearestDistance;

            ordered.add(new OptimizedWaypoint(
                    sequence++,
                    nearest.name(),
                    nearest.latitude(),
                    nearest.longitude(),
                    round(nearestDistance)
            ));

            currentLat = nearest.latitude();
            currentLon = nearest.longitude();
            remaining.remove(nearest);
        }

        return new OptimizeRouteResponse(ordered, round(totalDistance));
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0; // 2 decimal places
    }
}
