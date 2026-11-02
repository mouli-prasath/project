package com.tripplanner.dto.favorite;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// Favoriting a place the user found via search (not yet necessarily in our
// DB) - so we take enough info to find-or-create the Place row, same
// pattern as CreateActivityRequest.
public record CreateFavoriteRequest(
        @NotBlank String placeName,
        String address,
        String category,
        String description,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude
) {}
