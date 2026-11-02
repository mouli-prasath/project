package com.tripplanner.service.map;

import com.tripplanner.dto.map.OptimizeRouteRequest;
import com.tripplanner.dto.map.OptimizeRouteResponse;
import com.tripplanner.dto.map.Waypoint;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RouteOptimizationServiceTest {

    private final RouteOptimizationService service = new RouteOptimizationService();

    @Test
    void reordersWaypointsByProximityFromStart() {
        // Three points along the same meridian, each ~111km apart (1 degree
        // of latitude). Deliberately passed in a shuffled (worst-case) order
        // to prove the service actually reorders them, not just echoes input.
        Waypoint start = new Waypoint("Hotel", 0.0, 0.0);
        Waypoint near = new Waypoint("Near", 1.0, 0.0);
        Waypoint mid = new Waypoint("Mid", 2.0, 0.0);
        Waypoint far = new Waypoint("Far", 3.0, 0.0);

        OptimizeRouteRequest request = new OptimizeRouteRequest(start, List.of(far, near, mid));

        OptimizeRouteResponse response = service.optimize(request);

        List<String> orderedNames = response.route().stream().map(w -> w.name()).toList();
        assertEquals(List.of("Near", "Mid", "Far"), orderedNames);
    }

    @Test
    void sequenceNumbersAreOneIndexedAndContiguous() {
        Waypoint start = new Waypoint("Hotel", 0.0, 0.0);
        Waypoint a = new Waypoint("A", 1.0, 0.0);
        Waypoint b = new Waypoint("B", 2.0, 0.0);

        OptimizeRouteResponse response = service.optimize(new OptimizeRouteRequest(start, List.of(a, b)));

        assertEquals(1, response.route().get(0).sequence());
        assertEquals(2, response.route().get(1).sequence());
    }

    @Test
    void totalDistanceIsSumOfIndividualLegs() {
        Waypoint start = new Waypoint("Hotel", 0.0, 0.0);
        Waypoint a = new Waypoint("A", 1.0, 0.0);
        Waypoint b = new Waypoint("B", 2.0, 0.0);

        OptimizeRouteResponse response = service.optimize(new OptimizeRouteRequest(start, List.of(a, b)));

        double sumOfLegs = response.route().stream().mapToDouble(w -> w.legDistanceKm()).sum();
        assertEquals(sumOfLegs, response.totalDistanceKm(), 0.01);
    }

    @Test
    void singleWaypoint_returnsThatWaypointWithDistanceFromStart() {
        Waypoint start = new Waypoint("Hotel", 0.0, 0.0);
        Waypoint only = new Waypoint("OnlyStop", 1.0, 0.0);

        OptimizeRouteResponse response = service.optimize(new OptimizeRouteRequest(start, List.of(only)));

        assertEquals(1, response.route().size());
        assertEquals("OnlyStop", response.route().get(0).name());
        assertTrue(response.totalDistanceKm() > 0);
    }
}
