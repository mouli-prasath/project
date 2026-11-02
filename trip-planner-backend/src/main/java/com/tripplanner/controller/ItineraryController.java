package com.tripplanner.controller;

import com.tripplanner.dto.trip.*;
import com.tripplanner.security.UserPrincipal;
import com.tripplanner.service.ItineraryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequiredArgsConstructor
@Tag(name = "Itinerary", description = "View itinerary and edit individual activities")
public class ItineraryController {

    private final ItineraryService itineraryService;

    @GetMapping("/api/trips/{tripId}/itinerary")
    public ResponseEntity<ItineraryResponse> getItinerary(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long tripId
    ) {
        return ResponseEntity.ok(itineraryService.getItinerary(principal.getId(), tripId));
    }

    @PostMapping("/api/trips/{tripId}/activities")
    public ResponseEntity<ActivityResponse> addActivity(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long tripId,
            @Valid @RequestBody CreateActivityRequest request
    ) {
        ActivityResponse response = itineraryService.addActivity(principal.getId(), tripId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/api/activities/{activityId}")
    public ResponseEntity<ActivityResponse> updateActivity(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long activityId,
            @RequestBody UpdateActivityRequest request
    ) {
        return ResponseEntity.ok(itineraryService.updateActivity(principal.getId(), activityId, request));
    }

    @DeleteMapping("/api/activities/{activityId}")
    public ResponseEntity<Void> deleteActivity(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long activityId
    ) {
        itineraryService.deleteActivity(principal.getId(), activityId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/api/activities/{activityId}/reorder")
    public ResponseEntity<ActivityResponse> reorderActivity(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long activityId,
            @Valid @RequestBody ReorderActivityRequest request
    ) {
        return ResponseEntity.ok(itineraryService.reorderActivity(principal.getId(), activityId, request.newOrder()));
    }
}
