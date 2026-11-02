package com.tripplanner.service;

import com.tripplanner.dto.ai.GeneratedActivity;
import com.tripplanner.dto.ai.GeneratedDay;
import com.tripplanner.dto.ai.GeneratedItinerary;
import com.tripplanner.dto.map.PlaceSearchResult;
import com.tripplanner.dto.trip.*;
import com.tripplanner.entity.*;
import com.tripplanner.exception.BadRequestException;
import com.tripplanner.exception.ResourceNotFoundException;
import com.tripplanner.repository.*;
import com.tripplanner.service.ai.AiItineraryService;
import com.tripplanner.service.map.OsmMapsClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ItineraryService {

    private final TripRepository tripRepository;
    private final TripPreferenceRepository tripPreferenceRepository;
    private final ItineraryDayRepository itineraryDayRepository;
    private final ActivityRepository activityRepository;

    private final OsmMapsClient mapsClient;
    private final AiItineraryService aiItineraryService;
    private final PlaceService placeService;

    // ---------- Generation ----------

    @Transactional
    public ItineraryResponse generate(Long userId, Long tripId) {
        Trip trip = findOwnedTrip(userId, tripId);

        if (trip.getStatus() == TripStatus.GENERATED) {
            throw new BadRequestException(
                    "This trip already has an itinerary. Use POST /api/trips/" + tripId + "/regenerate instead.");
        }

        return generateAndPersist(trip, false);
    }

    @Transactional
    public ItineraryResponse regenerate(Long userId, Long tripId, boolean optimizeForBudget) {
        Trip trip = findOwnedTrip(userId, tripId);
        clearExistingItinerary(tripId);
        return generateAndPersist(trip, optimizeForBudget);
    }

    @Transactional
    public ItineraryDayResponse regenerateDay(Long userId, Long tripId, int dayNumber) {
        Trip trip = findOwnedTrip(userId, tripId);

        ItineraryDay existingDay = itineraryDayRepository.findByTripIdAndDayNumber(tripId, dayNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Day " + dayNumber + " does not exist for this trip. Generate the full itinerary first."));

        TripPreference preference = tripPreferenceRepository.findByTripId(tripId).orElse(null);
        List<PlaceSearchResult> candidatePlaces = fetchCandidatePlaces(trip, preference);

        GeneratedItinerary generated = aiItineraryService.regenerateSingleDay(
                trip, preference, candidatePlaces, dayNumber, existingDay.getDate());

        if (generated.days() == null || generated.days().isEmpty()) {
            throw new BadRequestException("AI did not return a day to replace day " + dayNumber);
        }

        // Replace this day's activities only - other days are untouched.
        existingDay.getActivities().clear(); // orphanRemoval deletes the old rows
        activityRepository.flush();

        GeneratedDay newDay = generated.days().get(0);
        existingDay.setTheme(newDay.theme());
        persistActivitiesForDay(existingDay, newDay.activities(), candidatePlaces);

        ItineraryDay saved = itineraryDayRepository.save(existingDay);
        return ItineraryDayResponse.from(saved);
    }

    private ItineraryResponse generateAndPersist(Trip trip, boolean optimizeForBudget) {
        TripPreference preference = tripPreferenceRepository.findByTripId(trip.getId()).orElse(null);
        List<PlaceSearchResult> candidatePlaces = fetchCandidatePlaces(trip, preference);

        if (candidatePlaces.isEmpty()) {
            trip.setStatus(TripStatus.FAILED);
            tripRepository.save(trip);
            throw new ResourceNotFoundException(
                    "No real places found for '" + trip.getDestination() + "'. Check the spelling and try again.");
        }

        GeneratedItinerary generated;
        try {
            generated = aiItineraryService.generateFullTrip(trip, preference, candidatePlaces, optimizeForBudget);
        } catch (RuntimeException e) {
            trip.setStatus(TripStatus.FAILED);
            tripRepository.save(trip);
            throw e;
        }

        trip.setTitle(generated.tripTitle());
        trip.setStatus(TripStatus.GENERATED);
        tripRepository.save(trip);

        List<ItineraryDayResponse> dayResponses = new ArrayList<>();
        for (GeneratedDay generatedDay : generated.days()) {
            ItineraryDay day = ItineraryDay.builder()
                    .trip(trip)
                    .dayNumber(generatedDay.dayNumber())
                    .date(parseDateOrFallback(generatedDay.date(), trip.getStartDate(), generatedDay.dayNumber()))
                    .theme(generatedDay.theme())
                    .build();

            persistActivitiesForDay(day, generatedDay.activities(), candidatePlaces);
            ItineraryDay saved = itineraryDayRepository.save(day);
            dayResponses.add(ItineraryDayResponse.from(saved));
        }

        return new ItineraryResponse(trip.getId(), generated.tripTitle(), generated.summary(), dayResponses);
    }

    private List<PlaceSearchResult> fetchCandidatePlaces(Trip trip, TripPreference preference) {
        List<String> interests = (preference != null && preference.getInterests() != null)
                ? Arrays.asList(preference.getInterests().split(","))
                : List.of();
        return mapsClient.gatherCandidatePlaces(trip.getDestination(), interests);
    }

    private void persistActivitiesForDay(
            ItineraryDay day, List<GeneratedActivity> activities, List<PlaceSearchResult> candidatePlaces
    ) {
        Map<String, PlaceSearchResult> candidatesByName = new HashMap<>();
        for (PlaceSearchResult p : candidatePlaces) {
            candidatesByName.putIfAbsent(p.name().toLowerCase().trim(), p);
        }

        int order = 1;
        for (GeneratedActivity ga : activities) {
            PlaceSearchResult matched = candidatesByName.get(ga.name().toLowerCase().trim());

            Place place = placeService.findOrCreate(
                    ga.name(),
                    ga.description(),
                    ga.category(),
                    ga.latitude(),
                    ga.longitude(),
                    ga.address(),
                    matched != null ? matched.openingTime() : null,
                    matched != null ? matched.closingTime() : null
            );

            Activity activity = Activity.builder()
                    .itineraryDay(day)
                    .place(place)
                    .startTime(parseTimeOrNull(ga.startTime()))
                    .durationMinutes(ga.durationMinutes())
                    .endTime(computeEndTime(ga.startTime(), ga.durationMinutes()))
                    .estimatedCost(ga.estimatedCost() != null ? BigDecimal.valueOf(ga.estimatedCost()) : null)
                    .description(ga.description())
                    .category(ga.category())
                    .activityOrder(order++)
                    .reason(ga.reason())
                    .build();

            day.getActivities().add(activity);
        }
    }

    // ---------- Read ----------

    @Transactional(readOnly = true)
    public ItineraryResponse getItinerary(Long userId, Long tripId) {
        Trip trip = findOwnedTrip(userId, tripId);
        List<ItineraryDay> days = itineraryDayRepository.findByTripIdOrderByDayNumberAsc(tripId);

        if (days.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No itinerary generated yet for this trip. Call POST /api/trips/" + tripId + "/generate first.");
        }

        List<ItineraryDayResponse> dayResponses = days.stream().map(ItineraryDayResponse::from).toList();
        return new ItineraryResponse(trip.getId(), trip.getTitle(), null, dayResponses);
    }

    // ---------- Activity-level editing ----------

    @Transactional
    public ActivityResponse addActivity(Long userId, Long tripId, CreateActivityRequest request) {
        findOwnedTrip(userId, tripId);

        ItineraryDay day = itineraryDayRepository.findByTripIdAndDayNumber(tripId, request.dayNumber())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Day " + request.dayNumber() + " does not exist for this trip"));

        Place place = placeService.findOrCreate(
                request.placeName(), request.description(), request.category(),
                request.latitude(), request.longitude(), request.address(), null, null);

        int nextOrder = day.getActivities().stream()
                .mapToInt(Activity::getActivityOrder).max().orElse(0) + 1;

        Activity activity = Activity.builder()
                .itineraryDay(day)
                .place(place)
                .startTime(request.startTime())
                .endTime(request.endTime())
                .durationMinutes(request.durationMinutes())
                .estimatedCost(request.estimatedCost())
                .description(request.description())
                .category(request.category())
                .activityOrder(nextOrder)
                .build();

        Activity saved = activityRepository.save(activity);
        return ActivityResponse.from(saved);
    }

    @Transactional
    public ActivityResponse updateActivity(Long userId, Long activityId, UpdateActivityRequest request) {
        Activity activity = findOwnedActivity(userId, activityId);

        if (request.startTime() != null) activity.setStartTime(request.startTime());
        if (request.endTime() != null) activity.setEndTime(request.endTime());
        if (request.durationMinutes() != null) activity.setDurationMinutes(request.durationMinutes());
        if (request.estimatedCost() != null) activity.setEstimatedCost(request.estimatedCost());
        if (request.description() != null) activity.setDescription(request.description());
        if (request.category() != null) activity.setCategory(request.category());

        return ActivityResponse.from(activityRepository.save(activity));
    }

    @Transactional
    public void deleteActivity(Long userId, Long activityId) {
        Activity activity = findOwnedActivity(userId, activityId);
        ItineraryDay day = activity.getItineraryDay();

        activityRepository.delete(activity);

        // Re-sequence remaining activities so order stays gapless (1..N).
        List<Activity> remaining = new ArrayList<>(day.getActivities());
        remaining.removeIf(a -> a.getId().equals(activityId));
        remaining.sort(Comparator.comparing(Activity::getActivityOrder));
        int order = 1;
        for (Activity a : remaining) {
            a.setActivityOrder(order++);
        }
        activityRepository.saveAll(remaining);
    }

    @Transactional
    public ActivityResponse reorderActivity(Long userId, Long activityId, int newOrder) {
        Activity activity = findOwnedActivity(userId, activityId);
        ItineraryDay day = activity.getItineraryDay();

        List<Activity> siblings = new ArrayList<>(day.getActivities());
        siblings.sort(Comparator.comparing(Activity::getActivityOrder));
        siblings.removeIf(a -> a.getId().equals(activityId));

        int clampedPosition = Math.max(1, Math.min(newOrder, siblings.size() + 1));
        siblings.add(clampedPosition - 1, activity);

        int order = 1;
        for (Activity a : siblings) {
            a.setActivityOrder(order++);
        }
        activityRepository.saveAll(siblings);

        return ActivityResponse.from(activity);
    }

    // ---------- Helpers ----------

    private Trip findOwnedTrip(Long userId, Long tripId) {
        return tripRepository.findByIdAndUserId(tripId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));
    }

    /**
     * Loads an activity and verifies (via its day -> trip -> user chain)
     * that it belongs to the requesting user. Returns 404 rather than 403
     * for the same ID-enumeration reason as findOwnedTrip.
     */
    private Activity findOwnedActivity(Long userId, Long activityId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Activity not found"));

        if (!activity.getItineraryDay().getTrip().getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Activity not found");
        }
        return activity;
    }

    private void clearExistingItinerary(Long tripId) {
        List<ItineraryDay> days = itineraryDayRepository.findByTripIdOrderByDayNumberAsc(tripId);
        itineraryDayRepository.deleteAll(days); // cascades to activities via orphanRemoval + DB ON DELETE CASCADE
    }

    private LocalDate parseDateOrFallback(String date, LocalDate tripStart, Integer dayNumber) {
        try {
            return LocalDate.parse(date);
        } catch (Exception e) {
            // AI gave a malformed date - derive it deterministically instead
            // of failing the whole generation over a formatting slip.
            return tripStart.plusDays(Math.max(0, (dayNumber != null ? dayNumber : 1) - 1));
        }
    }

    private LocalTime parseTimeOrNull(String time) {
        if (time == null || time.isBlank()) return null;
        try {
            String[] parts = time.split(":");
            return LocalTime.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        } catch (Exception e) {
            return null;
        }
    }

    private LocalTime computeEndTime(String startTime, Integer durationMinutes) {
        LocalTime start = parseTimeOrNull(startTime);
        if (start == null || durationMinutes == null) return null;
        return start.plusMinutes(durationMinutes);
    }
}
