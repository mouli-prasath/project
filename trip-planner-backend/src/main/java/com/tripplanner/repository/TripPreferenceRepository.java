package com.tripplanner.repository;

import com.tripplanner.entity.TripPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TripPreferenceRepository extends JpaRepository<TripPreference, Long> {
    Optional<TripPreference> findByTripId(Long tripId);
}
