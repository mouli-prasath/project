package com.tripplanner.repository;

import com.tripplanner.entity.Place;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlaceRepository extends JpaRepository<Place, Long> {
    // Used when caching AI/Places results to avoid duplicate rows for the same location.
    Optional<Place> findByNameAndLatitudeAndLongitude(String name, Double latitude, Double longitude);
}
