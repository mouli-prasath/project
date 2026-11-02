package com.tripplanner.service.ai;

import com.tripplanner.dto.ai.GeneratedActivity;
import com.tripplanner.dto.ai.GeneratedDay;
import com.tripplanner.dto.ai.GeneratedItinerary;
import com.tripplanner.dto.map.PlaceSearchResult;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Section 5.17 "AI validation / fallback" implemented as a pure function:
 * given what the AI returned and the real candidate places it was supposed
 * to choose from, return a list of problems (empty = valid). The caller
 * (AiItineraryService) decides what to do with problems - typically retry
 * once with a stricter prompt, then give up with a clear error.
 */
@Service
public class ItineraryValidationService {

    public List<String> validate(GeneratedItinerary itinerary, List<PlaceSearchResult> candidatePlaces) {
        List<String> errors = new ArrayList<>();

        if (itinerary == null || itinerary.days() == null || itinerary.days().isEmpty()) {
            errors.add("Response must contain at least one day with activities");
            return errors;
        }

        Map<String, PlaceSearchResult> candidatesByName = candidatePlaces.stream()
                .collect(Collectors.toMap(
                        p -> p.name().toLowerCase().trim(),
                        p -> p,
                        (a, b) -> a));

        for (GeneratedDay day : itinerary.days()) {
            if (day.activities() == null || day.activities().isEmpty()) {
                errors.add("Day " + day.dayNumber() + " has no activities");
                continue;
            }

            for (GeneratedActivity activity : day.activities()) {
                validateActivity(activity, day.dayNumber(), candidatesByName, errors);
            }
        }

        return errors;
    }

    private void validateActivity(
            GeneratedActivity activity,
            Integer dayNumber,
            Map<String, PlaceSearchResult> candidatesByName,
            List<String> errors
    ) {
        String label = "Day " + dayNumber + " activity '" + activity.name() + "'";

        if (activity.name() == null || activity.name().isBlank()) {
            errors.add("Day " + dayNumber + " has an activity with no name");
            return;
        }

        if (activity.latitude() == null || activity.longitude() == null) {
            errors.add(label + " is missing coordinates");
        } else if (activity.latitude() < -90 || activity.latitude() > 90
                || activity.longitude() < -180 || activity.longitude() > 180) {
            errors.add(label + " has out-of-range coordinates");
        }

        if (activity.estimatedCost() != null && activity.estimatedCost() < 0) {
            errors.add(label + " has a negative estimated cost");
        }

        if (activity.durationMinutes() != null && activity.durationMinutes() <= 0) {
            errors.add(label + " has a non-positive duration");
        }

        // Guard against hallucinated places: the AI must choose only from
        // the real places it was given, not invent new ones.
        PlaceSearchResult matched = candidatesByName.get(activity.name().toLowerCase().trim());
        if (matched == null) {
            errors.add(label + " is not one of the real places provided - "
                    + "likely hallucinated and must be replaced with one from the list");
            return; // no point checking opening hours against a place we don't recognize
        }

        // Opening-hours check ("Important Details" section): don't let the
        // AI schedule a visit outside the place's known hours. Skipped
        // gracefully when either side doesn't have hours data.
        if (matched.openingTime() != null && matched.closingTime() != null && activity.startTime() != null) {
            try {
                LocalTime start = LocalTime.parse(normalizeTime(activity.startTime()));
                LocalTime open = LocalTime.parse(matched.openingTime());
                LocalTime close = LocalTime.parse(matched.closingTime());

                boolean withinHours = close.isAfter(open)
                        ? (!start.isBefore(open) && !start.isAfter(close))
                        : true; // overnight hours (close < open) - skip strict check, too ambiguous to validate safely

                if (!withinHours) {
                    errors.add(label + " is scheduled at " + activity.startTime()
                            + " but the venue's hours are " + matched.openingTime()
                            + "-" + matched.closingTime());
                }
            } catch (DateTimeParseException e) {
                // Malformed time string from the AI - flag it explicitly
                // rather than silently ignoring.
                errors.add(label + " has an unparseable startTime: '" + activity.startTime() + "'");
            }
        }
    }

    private String normalizeTime(String time) {
        // Accepts "9:00" as well as "09:00".
        String[] parts = time.split(":");
        if (parts.length != 2) return time;
        return String.format("%02d:%s", Integer.parseInt(parts[0]), parts[1]);
    }
}
