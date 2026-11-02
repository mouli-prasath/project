package com.tripplanner.controller;

import com.tripplanner.dto.map.*;
import com.tripplanner.service.map.OsmMapsClient;
import com.tripplanner.service.map.RouteOptimizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/map")
@RequiredArgsConstructor
@Tag(name = "Map", description = "Geocoding, place search, and route optimization")
public class MapController {

    private final OsmMapsClient mapsClient;
    private final RouteOptimizationService routeOptimizationService;

    /**
     * Resolves a free-text address/place name to coordinates.
     * Used when creating a trip (turning "Chennai" into a real lat/lng),
     * and can back a destination-search autocomplete on the frontend.
     */
    @GetMapping("/geocode")
    public ResponseEntity<GeocodeResponse> geocode(@RequestParam String address) {
        return ResponseEntity.ok(mapsClient.geocode(address));
    }

    /**
     * Finds real places near a point, filtered by category. NOTE: this
     * contract changed from free-text search (e.g. "beaches in Goa") to
     * coordinates + category, because Overpass (OpenStreetMap's query API)
     * does structured tag-based search, not natural-language search the way
     * Google Places did. Recognized categories: BEACHES, FOOD, NIGHTLIFE,
     * HISTORY, CULTURE, ADVENTURE, NATURE, SHOPPING, PHOTOGRAPHY - anything
     * else (or omitted) falls back to general tourist attractions.
     */
    @GetMapping("/places/search")
    public ResponseEntity<List<PlaceSearchResult>> searchPlaces(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(defaultValue = "5") int radiusKm,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "10") int maxResults
    ) {
        int radiusMeters = Math.min(Math.max(radiusKm, 1), 20) * 1000;
        return ResponseEntity.ok(mapsClient.searchByCategory(latitude, longitude, radiusMeters, category, maxResults));
    }

    /**
     * Reorders a set of stops into an efficient visiting sequence using
     * nearest-neighbor optimization, starting from a fixed point (e.g. the
     * traveler's hotel). Pure computation, no external API call - safe to
     * call as often as needed while a user edits their day's plan.
     */
    @PostMapping("/optimize-route")
    public ResponseEntity<OptimizeRouteResponse> optimizeRoute(@Valid @RequestBody OptimizeRouteRequest request) {
        return ResponseEntity.ok(routeOptimizationService.optimize(request));
    }
}
