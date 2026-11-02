package com.tripplanner.service;

import com.tripplanner.dto.share.PublicTripResponse;
import com.tripplanner.dto.share.ShareLinkResponse;
import com.tripplanner.dto.trip.ItineraryDayResponse;
import com.tripplanner.entity.ItineraryDay;
import com.tripplanner.entity.Trip;
import com.tripplanner.exception.ResourceNotFoundException;
import com.tripplanner.repository.ItineraryDayRepository;
import com.tripplanner.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SharingService {

    private final TripRepository tripRepository;
    private final ItineraryDayRepository itineraryDayRepository;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    @Transactional
    public ShareLinkResponse createShareLink(Long userId, Long tripId) {
        Trip trip = tripRepository.findByIdAndUserId(tripId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));

        if (trip.getShareToken() == null) {
            trip.setShareToken(UUID.randomUUID().toString());
            tripRepository.save(trip);
        }

        return new ShareLinkResponse(
                trip.getShareToken(),
                frontendBaseUrl + "/shared/trips/" + trip.getShareToken()
        );
    }

    @Transactional(readOnly = true)
    public PublicTripResponse getPublicTrip(String shareToken) {
        Trip trip = tripRepository.findByShareToken(shareToken)
                .orElseThrow(() -> new ResourceNotFoundException("This share link is invalid or has expired"));

        List<ItineraryDay> days = itineraryDayRepository.findByTripIdOrderByDayNumberAsc(trip.getId());
        List<ItineraryDayResponse> dayResponses = days.stream().map(ItineraryDayResponse::from).toList();

        return new PublicTripResponse(
                trip.getTitle(), trip.getDestination(), trip.getStartDate(), trip.getEndDate(),
                trip.getTravelers(), trip.getBudget(), dayResponses
        );
    }
}
