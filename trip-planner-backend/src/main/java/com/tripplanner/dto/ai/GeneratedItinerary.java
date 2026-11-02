package com.tripplanner.dto.ai;

import java.util.List;

// Top-level shape the AI must return, matching the spec's "tripTitle / summary / days" schema.
public record GeneratedItinerary(
        String tripTitle,
        String summary,
        List<GeneratedDay> days
) {}
