package com.tripplanner.controller;

import com.tripplanner.dto.explore.DestinationResponse;
import com.tripplanner.dto.trip.PagedResponse;
import com.tripplanner.security.UserPrincipal;
import com.tripplanner.service.ExploreService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * All endpoints here are public (see SecurityConfig - /api/explore/** is
 * permitAll), matching the spec's navigation: "Explore" is visible to
 * logged-out users too. Where a valid JWT IS supplied, JwtAuthFilter still
 * resolves it and @AuthenticationPrincipal will be non-null, letting
 * /recommended personalize without requiring auth.
 */
@RestController
@RequestMapping("/api/explore")
@RequiredArgsConstructor
@Tag(name = "Explore", description = "Public destination discovery and search")
public class ExploreController {

    private final ExploreService exploreService;

    @GetMapping
    public ResponseEntity<PagedResponse<DestinationResponse>> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(exploreService.search(search, category, pageable));
    }

    @GetMapping("/popular")
    public ResponseEntity<PagedResponse<DestinationResponse>> popular(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(exploreService.popular(PageRequest.of(page, size)));
    }

    @GetMapping("/recommended")
    public ResponseEntity<PagedResponse<DestinationResponse>> recommended(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Long userId = principal != null ? principal.getId() : null;
        return ResponseEntity.ok(exploreService.recommended(userId, PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DestinationResponse> getDestination(@PathVariable Long id) {
        return ResponseEntity.ok(exploreService.getById(id));
    }
}
