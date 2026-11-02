package com.tripplanner.service;

import com.tripplanner.dto.explore.DestinationResponse;
import com.tripplanner.dto.trip.PagedResponse;
import com.tripplanner.entity.Destination;
import com.tripplanner.entity.Trip;
import com.tripplanner.entity.TripPreference;
import com.tripplanner.exception.ResourceNotFoundException;
import com.tripplanner.repository.DestinationRepository;
import com.tripplanner.repository.TripPreferenceRepository;
import com.tripplanner.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ExploreService {

    private final DestinationRepository destinationRepository;
    private final TripRepository tripRepository;
    private final TripPreferenceRepository tripPreferenceRepository;

    /** GET /api/explore - search + category filter + pagination, all optional. */
    @Transactional(readOnly = true)
    public PagedResponse<DestinationResponse> search(String query, String category, Pageable pageable) {
        Page<Destination> page;

        boolean hasQuery = query != null && !query.isBlank();
        boolean hasCategory = category != null && !category.isBlank();

        if (hasQuery && hasCategory) {
            page = destinationRepository.findByCategoryIgnoreCaseAndNameContainingIgnoreCase(category, query, pageable);
        } else if (hasQuery) {
            page = destinationRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                    query, query, pageable);
        } else if (hasCategory) {
            page = destinationRepository.findByCategoryIgnoreCase(category, pageable);
        } else {
            page = destinationRepository.findAllByOrderByPopularityScoreDesc(pageable);
        }

        return PagedResponse.from(page.map(DestinationResponse::from));
    }

    /** GET /api/explore/popular - always ranked by popularity, no filters. */
    @Transactional(readOnly = true)
    public PagedResponse<DestinationResponse> popular(Pageable pageable) {
        Page<Destination> page = destinationRepository.findAllByOrderByPopularityScoreDesc(pageable);
        return PagedResponse.from(page.map(DestinationResponse::from));
    }

    /**
     * GET /api/explore/recommended - personalized when a userId is provided
     * (looks at their most recent trip's interests and matches destination
     * categories), falls back to popular for anonymous users or users with
     * no trip history yet. This keeps the endpoint usable for both logged-out
     * browsing and a logged-in dashboard's "Recommended for you" section.
     */
    @Transactional(readOnly = true)
    public PagedResponse<DestinationResponse> recommended(Long userId, Pageable pageable) {
        if (userId == null) {
            return popular(pageable);
        }

        Pageable mostRecentFirst = PageRequest.of(0, 1,
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
        Page<Trip> recentTrips = tripRepository.findByUserId(userId, mostRecentFirst);

        if (recentTrips.isEmpty()) {
            return popular(pageable);
        }

        TripPreference preference = tripPreferenceRepository
                .findByTripId(recentTrips.getContent().get(0).getId())
                .orElse(null);

        if (preference == null || preference.getInterests() == null || preference.getInterests().isBlank()) {
            return popular(pageable);
        }

        Set<String> interestCategories = new LinkedHashSet<>(Arrays.asList(preference.getInterests().split(",")));
        List<Destination> matches = destinationRepository.findByCategoryIgnoreCaseIn(List.copyOf(interestCategories));

        if (matches.isEmpty()) {
            return popular(pageable);
        }

        // Simple in-memory pagination over the matched set - the candidate
        // pool here is small (a curated catalog, not millions of rows), so
        // this is fine without pushing pagination into the query itself.
        int start = Math.min((int) pageable.getOffset(), matches.size());
        int end = Math.min(start + pageable.getPageSize(), matches.size());
        List<DestinationResponse> pageContent = matches.subList(start, end).stream()
                .map(DestinationResponse::from).toList();

        org.springframework.data.domain.Page<DestinationResponse> page =
                new org.springframework.data.domain.PageImpl<>(pageContent, pageable, matches.size());

        return PagedResponse.from(page);
    }

    @Transactional(readOnly = true)
    public DestinationResponse getById(Long id) {
        Destination destination = destinationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Destination not found"));
        return DestinationResponse.from(destination);
    }
}
