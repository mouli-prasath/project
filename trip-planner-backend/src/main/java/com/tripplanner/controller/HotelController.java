package com.tripplanner.controller;

import com.tripplanner.dto.hotel.HotelSearchResponse;
import com.tripplanner.security.UserPrincipal;
import com.tripplanner.service.hotel.HotelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/trips/{tripId}/hotels")
@RequiredArgsConstructor
@Tag(name = "Hotels", description = "Nearby hotel discovery via Google Places")
public class HotelController {

    private final HotelService hotelService;

    /**
     * Discovers real hotels near the trip's itinerary (or its destination,
     * if no itinerary has been generated yet). All filters are optional and
     * only apply where Google's data supports them (see HotelService).
     */
    @GetMapping
    public ResponseEntity<HotelSearchResponse> searchHotels(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long tripId,
            @RequestParam(defaultValue = "5") int radiusKm,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) Double maxDistanceKm,
            @RequestParam(required = false) String maxPriceLevel,
            @RequestParam(required = false) String amenities // comma-separated, e.g. "pool,parking"
    ) {
        List<String> amenityList = (amenities == null || amenities.isBlank())
                ? List.of()
                : Arrays.stream(amenities.split(",")).map(String::trim).toList();

        HotelSearchResponse response = hotelService.search(
                principal.getId(), tripId, radiusKm, minRating, maxDistanceKm, maxPriceLevel, amenityList
        );
        return ResponseEntity.ok(response);
    }
}
