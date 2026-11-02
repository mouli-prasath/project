package com.tripplanner.service;

import com.tripplanner.dto.trip.CreateTripRequest;
import com.tripplanner.dto.trip.TripResponse;
import com.tripplanner.dto.trip.UpdateTripRequest;
import com.tripplanner.entity.Trip;
import com.tripplanner.entity.TripPreference;
import com.tripplanner.entity.TripStatus;
import com.tripplanner.entity.User;
import com.tripplanner.exception.BadRequestException;
import com.tripplanner.exception.ResourceNotFoundException;
import com.tripplanner.repository.TripPreferenceRepository;
import com.tripplanner.repository.TripRepository;
import com.tripplanner.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TripService {

    private static final long MAX_TRIP_DURATION_DAYS = 30;

    private final TripRepository tripRepository;
    private final TripPreferenceRepository tripPreferenceRepository;
    private final UserRepository userRepository;

    @Transactional
    public TripResponse createTrip(Long userId, CreateTripRequest request) {
        validateDates(request.startDate(), request.endDate());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String title = (request.title() != null && !request.title().isBlank())
                ? request.title()
                : buildDefaultTitle(request);

        Trip trip = Trip.builder()
                .user(user)
                .title(title)
                .source(request.source())
                .destination(request.destination())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .travelers(request.travelers())
                .budget(request.budget())
                .travelStyle(request.travelStyle())
                .status(TripStatus.DRAFT)
                .preferredTransport(request.preferredTransport())
                .accommodationPreference(request.accommodationPreference())
                .build();

        Trip savedTrip = tripRepository.save(trip);

        String interestsCsv = (request.interests() == null || request.interests().isEmpty())
                ? null
                : String.join(",", request.interests());

        TripPreference preference = TripPreference.builder()
                .trip(savedTrip)
                .interests(interestsCsv)
                .additionalPreferences(request.additionalPreferences())
                .build();
        tripPreferenceRepository.save(preference);

        return TripResponse.from(savedTrip, interestsCsv, request.additionalPreferences());
    }

    @Transactional(readOnly = true)
    public Page<TripResponse> listTrips(Long userId, Pageable pageable) {
        return tripRepository.findByUserId(userId, pageable)
                .map(trip -> {
                    TripPreference pref = tripPreferenceRepository.findByTripId(trip.getId()).orElse(null);
                    String interests = pref != null ? pref.getInterests() : null;
                    String additional = pref != null ? pref.getAdditionalPreferences() : null;
                    return TripResponse.from(trip, interests, additional);
                });
    }

    @Transactional(readOnly = true)
    public TripResponse getTrip(Long userId, Long tripId) {
        Trip trip = findOwnedTrip(userId, tripId);
        TripPreference pref = tripPreferenceRepository.findByTripId(tripId).orElse(null);
        return TripResponse.from(trip, pref != null ? pref.getInterests() : null,
                pref != null ? pref.getAdditionalPreferences() : null);
    }

    @Transactional
    public TripResponse updateTrip(Long userId, Long tripId, UpdateTripRequest request) {
        Trip trip = findOwnedTrip(userId, tripId);

        if (request.startDate() != null) trip.setStartDate(request.startDate());
        if (request.endDate() != null) trip.setEndDate(request.endDate());
        validateDates(trip.getStartDate(), trip.getEndDate());

        if (request.title() != null) trip.setTitle(request.title());
        if (request.travelers() != null) trip.setTravelers(request.travelers());
        if (request.budget() != null) trip.setBudget(request.budget());
        if (request.travelStyle() != null) trip.setTravelStyle(request.travelStyle());
        if (request.preferredTransport() != null) trip.setPreferredTransport(request.preferredTransport());
        if (request.accommodationPreference() != null) trip.setAccommodationPreference(request.accommodationPreference());

        Trip updated = tripRepository.save(trip);

        TripPreference pref = tripPreferenceRepository.findByTripId(tripId)
                .orElse(TripPreference.builder().trip(trip).build());

        if (request.interests() != null) {
            pref.setInterests(request.interests().isEmpty() ? null : String.join(",", request.interests()));
        }
        if (request.additionalPreferences() != null) {
            pref.setAdditionalPreferences(request.additionalPreferences());
        }
        tripPreferenceRepository.save(pref);

        return TripResponse.from(updated, pref.getInterests(), pref.getAdditionalPreferences());
    }

    @Transactional
    public void deleteTrip(Long userId, Long tripId) {
        Trip trip = findOwnedTrip(userId, tripId);
        tripRepository.delete(trip); // cascades to preferences/days/activities via FK ON DELETE CASCADE
    }

    /**
     * Loads a trip and verifies it belongs to the requesting user.
     * Throws 404 (not 403) when the trip doesn't belong to the user, so we
     * don't leak whether a given trip ID exists at all to someone probing IDs.
     */
    private Trip findOwnedTrip(Long userId, Long tripId) {
        return tripRepository.findByIdAndUserId(tripId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));
    }

    private void validateDates(java.time.LocalDate start, java.time.LocalDate end) {
        if (end.isBefore(start)) {
            throw new BadRequestException("endDate cannot be before startDate");
        }
        long days = ChronoUnit.DAYS.between(start, end) + 1;
        if (days > MAX_TRIP_DURATION_DAYS) {
            throw new BadRequestException(
                    "Trip duration cannot exceed " + MAX_TRIP_DURATION_DAYS + " days");
        }
    }

    private String buildDefaultTitle(CreateTripRequest request) {
        long days = ChronoUnit.DAYS.between(request.startDate(), request.endDate()) + 1;
        return request.destination() + " — " + days + " Day Trip";
    }
}
