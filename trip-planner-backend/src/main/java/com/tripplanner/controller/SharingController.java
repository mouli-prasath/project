package com.tripplanner.controller;

import com.tripplanner.dto.share.PublicTripResponse;
import com.tripplanner.dto.share.ShareLinkResponse;
import com.tripplanner.security.UserPrincipal;
import com.tripplanner.service.SharingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequiredArgsConstructor
@Tag(name = "Sharing", description = "Public read-only trip links")
public class SharingController {

    private final SharingService sharingService;

    /** Owner-only: creates (or returns the existing) public link for a trip. */
    @PostMapping("/api/trips/{tripId}/share")
    public ResponseEntity<ShareLinkResponse> createShareLink(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long tripId
    ) {
        return ResponseEntity.ok(sharingService.createShareLink(principal.getId(), tripId));
    }

    /**
     * Public, unauthenticated: anyone with the link can view (read-only).
     * Registered under /api/shared/** which SecurityConfig permits without a JWT.
     */
    @GetMapping("/api/shared/trips/{shareToken}")
    public ResponseEntity<PublicTripResponse> getSharedTrip(@PathVariable String shareToken) {
        return ResponseEntity.ok(sharingService.getPublicTrip(shareToken));
    }
}
