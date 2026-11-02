package com.tripplanner.repository;

import com.tripplanner.entity.Trip;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TripRepository extends JpaRepository<Trip, Long> {
    Page<Trip> findByUserId(Long userId, Pageable pageable);
    Optional<Trip> findByIdAndUserId(Long id, Long userId);
    Optional<Trip> findByShareToken(String shareToken);
}
