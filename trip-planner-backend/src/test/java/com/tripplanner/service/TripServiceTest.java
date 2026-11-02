package com.tripplanner.service;

import com.tripplanner.dto.trip.CreateTripRequest;
import com.tripplanner.dto.trip.TripResponse;
import com.tripplanner.entity.TravelStyle;
import com.tripplanner.entity.Trip;
import com.tripplanner.entity.User;
import com.tripplanner.exception.BadRequestException;
import com.tripplanner.exception.ResourceNotFoundException;
import com.tripplanner.repository.TripPreferenceRepository;
import com.tripplanner.repository.TripRepository;
import com.tripplanner.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TripServiceTest {

    @Mock private TripRepository tripRepository;
    @Mock private TripPreferenceRepository tripPreferenceRepository;
    @Mock private UserRepository userRepository;

    private TripService tripService;

    @BeforeEach
    void setUp() {
        tripService = new TripService(tripRepository, tripPreferenceRepository, userRepository);
    }

    private CreateTripRequest validRequest(LocalDate start, LocalDate end) {
        return new CreateTripRequest(
                "Chennai", "Goa", start, end, 2, BigDecimal.valueOf(25000),
                TravelStyle.RELAXED, List.of("BEACHES", "FOOD"), "Prefer quiet places",
                null, null, null);
    }

    @Test
    void createTrip_endDateBeforeStartDate_throwsBadRequest() {
        CreateTripRequest request = validRequest(LocalDate.now().plusDays(10), LocalDate.now().plusDays(5));

        assertThrows(BadRequestException.class, () -> tripService.createTrip(1L, request));
        verifyNoInteractions(userRepository); // date validation should fail before touching the DB
    }

    @Test
    void createTrip_durationOver30Days_throwsBadRequest() {
        CreateTripRequest request = validRequest(LocalDate.now().plusDays(1), LocalDate.now().plusDays(40));

        assertThrows(BadRequestException.class, () -> tripService.createTrip(1L, request));
    }

    @Test
    void createTrip_validRequest_savesAndReturnsResponse() {
        LocalDate start = LocalDate.now().plusDays(5);
        LocalDate end = LocalDate.now().plusDays(8);
        CreateTripRequest request = validRequest(start, end);

        User user = User.builder().id(1L).name("Alex").email("alex@example.com").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tripPreferenceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TripResponse response = tripService.createTrip(1L, request);

        assertEquals("Goa", response.destination());
        assertEquals("Chennai", response.source());
        assertEquals(List.of("BEACHES", "FOOD"), response.interests());
        verify(tripRepository).save(any(Trip.class));
    }

    @Test
    void createTrip_userNotFound_throwsResourceNotFound() {
        CreateTripRequest request = validRequest(LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> tripService.createTrip(99L, request));
    }

    @Test
    void getTrip_notOwnedByRequestingUser_throwsResourceNotFound() {
        // Simulates user 2 trying to view user 1's trip by guessing the ID -
        // must come back as 404, not 403 (see TripService javadoc on this).
        when(tripRepository.findByIdAndUserId(5L, 2L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> tripService.getTrip(2L, 5L));
    }

    @Test
    void deleteTrip_notOwnedByRequestingUser_throwsResourceNotFound() {
        when(tripRepository.findByIdAndUserId(5L, 2L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> tripService.deleteTrip(2L, 5L));
        verify(tripRepository, never()).delete(any());
    }

    @Test
    void deleteTrip_ownedByRequestingUser_deletesSuccessfully() {
        Trip trip = Trip.builder().id(5L).destination("Goa").build();
        when(tripRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(trip));

        tripService.deleteTrip(1L, 5L);

        verify(tripRepository).delete(trip);
    }
}
