package com.tripplanner.dto.trip;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

// PUT /api/activities/{id}/reorder - moves an activity to a new position
// within its day; siblings are shifted automatically to keep order gapless.
public record ReorderActivityRequest(
        @NotNull @Min(1) Integer newOrder
) {}
