package com.tripplanner.dto.trip;

import com.tripplanner.entity.Activity;

import java.math.BigDecimal;
import java.time.LocalTime;

public record ActivityResponse(
        Long id,
        Long placeId,
        String placeName,
        String address,
        Double latitude,
        Double longitude,
        LocalTime startTime,
        LocalTime endTime,
        Integer durationMinutes,
        BigDecimal estimatedCost,
        String description,
        String category,
        Integer order,
        String reason,
        String weatherNote
) {
    public static ActivityResponse from(Activity activity) {
        return new ActivityResponse(
                activity.getId(),
                activity.getPlace().getId(),
                activity.getPlace().getName(),
                activity.getPlace().getAddress(),
                activity.getPlace().getLatitude(),
                activity.getPlace().getLongitude(),
                activity.getStartTime(),
                activity.getEndTime(),
                activity.getDurationMinutes(),
                activity.getEstimatedCost(),
                activity.getDescription(),
                activity.getCategory(),
                activity.getActivityOrder(),
                activity.getReason(),
                activity.getWeatherNote()
        );
    }
}
