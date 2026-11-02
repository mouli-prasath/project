package com.tripplanner.repository;

import com.tripplanner.entity.Destination;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DestinationRepository extends JpaRepository<Destination, Long> {

    Page<Destination> findAllByOrderByPopularityScoreDesc(Pageable pageable);

    Page<Destination> findByCategoryIgnoreCase(String category, Pageable pageable);

    Page<Destination> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            String nameQuery, String descriptionQuery, Pageable pageable);

    Page<Destination> findByCategoryIgnoreCaseAndNameContainingIgnoreCase(
            String category, String nameQuery, Pageable pageable);

    java.util.List<Destination> findByCategoryIgnoreCaseIn(java.util.List<String> categories);
}
