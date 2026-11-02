package com.tripplanner.service;

import com.tripplanner.entity.Place;
import com.tripplanner.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;

@Service
@RequiredArgsConstructor
public class PlaceService {

    private final PlaceRepository placeRepository;

    /**
     * Finds an existing Place matching this name+coordinates, or creates one.
     * Places are shared/reused across trips and users rather than duplicated
     * per-activity, so favoriting or re-visiting the same place stays
     * consistent.
     */
    @Transactional
    public Place findOrCreate(
            String name, String description, String category,
            Double latitude, Double longitude, String address,
            String openingTime, String closingTime
    ) {
        return placeRepository.findByNameAndLatitudeAndLongitude(name, latitude, longitude)
                .orElseGet(() -> placeRepository.save(Place.builder()
                        .name(name)
                        .description(description)
                        .category(category)
                        .latitude(latitude)
                        .longitude(longitude)
                        .address(address)
                        .openingTime(parseTimeOrNull(openingTime))
                        .closingTime(parseTimeOrNull(closingTime))
                        .build()));
    }

    private LocalTime parseTimeOrNull(String time) {
        if (time == null || time.isBlank()) return null;
        try {
            return LocalTime.parse(time);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
