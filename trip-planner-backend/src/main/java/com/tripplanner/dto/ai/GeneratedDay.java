package com.tripplanner.dto.ai;

import java.util.List;

public record GeneratedDay(
        Integer dayNumber,
        String date,          // "YYYY-MM-DD"
        String theme,
        List<GeneratedActivity> activities
) {}
