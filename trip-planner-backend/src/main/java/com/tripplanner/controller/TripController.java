package com.tripplanner.controller;

import com.tripplanner.dto.trip.CreateTripRequest;
import com.tripplanner.dto.trip.ItineraryDayResponse;
import com.tripplanner.dto.trip.ItineraryResponse;
import com.tripplanner.dto.trip.PagedResponse;
import com.tripplanner.dto.trip.TripResponse;
import com.tripplanner.dto.trip.UpdateTripRequest;
import com.tripplanner.security.UserPrincipal;
import com.tripplanner.service.ItineraryService;
import com.tripplanner.service.TripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
@Tag(name = "Trips", description = "Trip CRUD and AI itinerary generation/regeneration")
public class TripController {

    private final TripService tripService;
    private final ItineraryService itineraryService;

    @PostMapping
    public ResponseEntity<TripResponse> createTrip(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateTripRequest request
    ) {
        TripResponse response = tripService.createTrip(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<PagedResponse<TripResponse>> listTrips(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        var result = tripService.listTrips(principal.getId(), pageable);
        return ResponseEntity.ok(PagedResponse.from(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TripResponse> getTrip(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(tripService.getTrip(principal.getId(), id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TripResponse> updateTrip(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestBody UpdateTripRequest request
    ) {
        return ResponseEntity.ok(tripService.updateTrip(principal.getId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTrip(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        tripService.deleteTrip(principal.getId(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/generate")
    public ResponseEntity<ItineraryResponse> generateItinerary(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(itineraryService.generate(principal.getId(), id));
    }

    @PostMapping("/{id}/regenerate")
    public ResponseEntity<ItineraryResponse> regenerateItinerary(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(itineraryService.regenerate(principal.getId(), id, false));
    }

    @PostMapping("/{id}/days/{dayNumber}/regenerate")
    public ResponseEntity<ItineraryDayResponse> regenerateDay(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @PathVariable int dayNumber
    ) {
        return ResponseEntity.ok(itineraryService.regenerateDay(principal.getId(), id, dayNumber));
    }
}
