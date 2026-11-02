package com.tripplanner.service.ai;

import com.tripplanner.dto.ai.GeneratedActivity;
import com.tripplanner.dto.ai.GeneratedDay;
import com.tripplanner.dto.ai.GeneratedItinerary;
import com.tripplanner.dto.map.PlaceSearchResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ItineraryValidationServiceTest {

    private final ItineraryValidationService service = new ItineraryValidationService();

    private final PlaceSearchResult fortAguada = new PlaceSearchResult(
            "place1", "Fort Aguada", "Goa", "historic site", null,
            15.4925, 73.7738, "09:00", "18:00");

    private final PlaceSearchResult bagaBeach = new PlaceSearchResult(
            "place2", "Baga Beach", "Goa", "beach", null,
            15.5553, 73.7517, null, null);

    @Test
    void validItinerary_producesNoErrors() {
        GeneratedActivity activity = new GeneratedActivity(
                "Fort Aguada", "historic site", "Historic Portuguese fort",
                "10:00", 120, 100.0, 15.4925, 73.7738, "Goa", "Matches history interest");

        GeneratedItinerary itinerary = new GeneratedItinerary(
                "Goa Trip", "A relaxed trip",
                List.of(new GeneratedDay(1, "2026-09-10", "Fort day", List.of(activity))));

        List<String> errors = service.validate(itinerary, List.of(fortAguada, bagaBeach));

        assertTrue(errors.isEmpty(), "Expected no errors but got: " + errors);
    }

    @Test
    void emptyDaysList_producesError() {
        GeneratedItinerary itinerary = new GeneratedItinerary("Trip", "Summary", List.of());

        List<String> errors = service.validate(itinerary, List.of(fortAguada));

        assertFalse(errors.isEmpty());
    }

    @Test
    void hallucinatedPlaceName_isCaught() {
        GeneratedActivity activity = new GeneratedActivity(
                "Made Up Palace That Does Not Exist", "historic site", "desc",
                "10:00", 60, 50.0, 15.5, 73.8, "Goa", "reason");

        GeneratedItinerary itinerary = new GeneratedItinerary(
                "Trip", "Summary",
                List.of(new GeneratedDay(1, "2026-09-10", "Day 1", List.of(activity))));

        List<String> errors = service.validate(itinerary, List.of(fortAguada, bagaBeach));

        assertTrue(errors.stream().anyMatch(e -> e.contains("not one of the real places")));
    }

    @Test
    void missingCoordinates_isCaught() {
        GeneratedActivity activity = new GeneratedActivity(
                "Fort Aguada", "historic site", "desc",
                "10:00", 60, 50.0, null, null, "Goa", "reason");

        GeneratedItinerary itinerary = new GeneratedItinerary(
                "Trip", "Summary",
                List.of(new GeneratedDay(1, "2026-09-10", "Day 1", List.of(activity))));

        List<String> errors = service.validate(itinerary, List.of(fortAguada));

        assertTrue(errors.stream().anyMatch(e -> e.contains("missing coordinates")));
    }

    @Test
    void outOfRangeLatitude_isCaught() {
        GeneratedActivity activity = new GeneratedActivity(
                "Fort Aguada", "historic site", "desc",
                "10:00", 60, 50.0, 999.0, 73.7738, "Goa", "reason");

        GeneratedItinerary itinerary = new GeneratedItinerary(
                "Trip", "Summary",
                List.of(new GeneratedDay(1, "2026-09-10", "Day 1", List.of(activity))));

        List<String> errors = service.validate(itinerary, List.of(fortAguada));

        assertTrue(errors.stream().anyMatch(e -> e.contains("out-of-range coordinates")));
    }

    @Test
    void negativeCost_isCaught() {
        GeneratedActivity activity = new GeneratedActivity(
                "Fort Aguada", "historic site", "desc",
                "10:00", 60, -50.0, 15.4925, 73.7738, "Goa", "reason");

        GeneratedItinerary itinerary = new GeneratedItinerary(
                "Trip", "Summary",
                List.of(new GeneratedDay(1, "2026-09-10", "Day 1", List.of(activity))));

        List<String> errors = service.validate(itinerary, List.of(fortAguada));

        assertTrue(errors.stream().anyMatch(e -> e.contains("negative estimated cost")));
    }

    @Test
    void nonPositiveDuration_isCaught() {
        GeneratedActivity activity = new GeneratedActivity(
                "Fort Aguada", "historic site", "desc",
                "10:00", 0, 50.0, 15.4925, 73.7738, "Goa", "reason");

        GeneratedItinerary itinerary = new GeneratedItinerary(
                "Trip", "Summary",
                List.of(new GeneratedDay(1, "2026-09-10", "Day 1", List.of(activity))));

        List<String> errors = service.validate(itinerary, List.of(fortAguada));

        assertTrue(errors.stream().anyMatch(e -> e.contains("non-positive duration")));
    }

    @Test
    void activityOutsideOpeningHours_isCaught() {
        // Fort Aguada's hours are 09:00-18:00 in the test fixture above.
        GeneratedActivity activity = new GeneratedActivity(
                "Fort Aguada", "historic site", "desc",
                "20:00", 60, 50.0, 15.4925, 73.7738, "Goa", "reason");

        GeneratedItinerary itinerary = new GeneratedItinerary(
                "Trip", "Summary",
                List.of(new GeneratedDay(1, "2026-09-10", "Day 1", List.of(activity))));

        List<String> errors = service.validate(itinerary, List.of(fortAguada));

        assertTrue(errors.stream().anyMatch(e -> e.contains("hours are")));
    }

    @Test
    void activityWithinOpeningHours_producesNoHoursError() {
        GeneratedActivity activity = new GeneratedActivity(
                "Fort Aguada", "historic site", "desc",
                "14:00", 60, 50.0, 15.4925, 73.7738, "Goa", "reason");

        GeneratedItinerary itinerary = new GeneratedItinerary(
                "Trip", "Summary",
                List.of(new GeneratedDay(1, "2026-09-10", "Day 1", List.of(activity))));

        List<String> errors = service.validate(itinerary, List.of(fortAguada));

        assertTrue(errors.stream().noneMatch(e -> e.contains("hours are")));
    }

    @Test
    void placeWithNoOpeningHoursData_skipsHoursCheckGracefully() {
        // bagaBeach fixture has null opening/closing times - should never
        // produce an hours-related error regardless of the scheduled time.
        GeneratedActivity activity = new GeneratedActivity(
                "Baga Beach", "beach", "desc",
                "23:00", 60, 0.0, 15.5553, 73.7517, "Goa", "reason");

        GeneratedItinerary itinerary = new GeneratedItinerary(
                "Trip", "Summary",
                List.of(new GeneratedDay(1, "2026-09-10", "Day 1", List.of(activity))));

        List<String> errors = service.validate(itinerary, List.of(bagaBeach));

        assertTrue(errors.isEmpty());
    }

    @Test
    void emptyActivitiesInADay_isCaught() {
        GeneratedItinerary itinerary = new GeneratedItinerary(
                "Trip", "Summary",
                List.of(new GeneratedDay(1, "2026-09-10", "Day 1", List.of())));

        List<String> errors = service.validate(itinerary, List.of(fortAguada));

        assertTrue(errors.stream().anyMatch(e -> e.contains("no activities")));
    }
}
