package com.tripplanner.service.ai;

import com.tripplanner.dto.ai.GeneratedItinerary;
import com.tripplanner.dto.map.PlaceSearchResult;
import com.tripplanner.dto.weather.WeatherForecast;
import com.tripplanner.entity.Trip;
import com.tripplanner.entity.TripPreference;
import com.tripplanner.exception.ExternalServiceException;
import com.tripplanner.service.weather.WeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AiItineraryService {

    private static final int MAX_RETRIES = 1; // one retry with a stricter prompt, then give up

    private final GeminiClient geminiClient;
    private final ItineraryValidationService validationService;
    private final WeatherService weatherService;

    /** Generates (or regenerates) the full trip itinerary. */
    public GeneratedItinerary generateFullTrip(
            Trip trip, TripPreference preference, List<PlaceSearchResult> candidatePlaces, boolean optimizeForBudget
    ) {
        String prompt = buildFullTripPrompt(trip, preference, candidatePlaces, optimizeForBudget);
        return generateWithValidation(prompt, candidatePlaces, 0);
    }

    /** Regenerates a single day, given the same candidate place pool. */
    public GeneratedItinerary regenerateSingleDay(
            Trip trip, TripPreference preference, List<PlaceSearchResult> candidatePlaces,
            int dayNumber, LocalDate date
    ) {
        String prompt = buildSingleDayPrompt(trip, preference, candidatePlaces, dayNumber, date);
        return generateWithValidation(prompt, candidatePlaces, 0);
    }

    private GeneratedItinerary generateWithValidation(
            String prompt, List<PlaceSearchResult> candidatePlaces, int attempt
    ) {
        GeneratedItinerary itinerary = geminiClient.generateItinerary(prompt);
        List<String> errors = validationService.validate(itinerary, candidatePlaces);

        if (errors.isEmpty()) {
            return itinerary;
        }

        if (attempt < MAX_RETRIES) {
            String stricterPrompt = prompt + "\n\n"
                    + "Your previous response had these problems - fix ALL of them exactly and "
                    + "respond with corrected JSON only:\n- " + String.join("\n- ", errors);
            return generateWithValidation(stricterPrompt, candidatePlaces, attempt + 1);
        }

        throw new ExternalServiceException(
                "AI itinerary failed validation after retry: " + String.join("; ", errors));
    }

    private String buildFullTripPrompt(
            Trip trip, TripPreference preference, List<PlaceSearchResult> candidatePlaces, boolean optimizeForBudget
    ) {
        long numDays = ChronoUnit.DAYS.between(trip.getStartDate(), trip.getEndDate()) + 1;

        StringBuilder prompt = new StringBuilder();
        prompt.append(systemInstructions());
        prompt.append("\nBuild a ").append(numDays).append("-day itinerary.\n");
        prompt.append(tripDetailsBlock(trip, preference));
        if (optimizeForBudget) {
            prompt.append("\nIMPORTANT: the previous itinerary exceeded the budget. ")
                  .append("Prioritize lower-cost activities and free/cheap attractions this time.\n");
        }
        prompt.append(weatherBlock(trip, candidatePlaces));
        prompt.append(candidatePlacesBlock(candidatePlaces));
        prompt.append(responseSchemaBlock());

        return prompt.toString();
    }

    private String buildSingleDayPrompt(
            Trip trip, TripPreference preference, List<PlaceSearchResult> candidatePlaces,
            int dayNumber, LocalDate date
    ) {
        StringBuilder prompt = new StringBuilder();
        prompt.append(systemInstructions());
        prompt.append("\nBuild a plan for exactly ONE day: day ").append(dayNumber)
              .append(" (").append(date).append(") of the trip.\n");
        prompt.append(tripDetailsBlock(trip, preference));
        prompt.append(weatherBlock(trip, candidatePlaces));
        prompt.append(candidatePlacesBlock(candidatePlaces));
        prompt.append("\nYour response's \"days\" array must contain EXACTLY ONE entry, ")
              .append("with dayNumber ").append(dayNumber).append(" and date \"").append(date).append("\".\n");
        prompt.append(responseSchemaBlock());

        return prompt.toString();
    }

    private String systemInstructions() {
        return "You are a professional travel planner. You must build itineraries ONLY using "
                + "the real places provided in the candidate list below. Never invent place names "
                + "that aren't in that list. For every activity, briefly explain WHY it fits this "
                + "traveler (their interests, budget, or proximity) in the 'reason' field. "
                + "Respond with valid JSON only - no markdown fences, no commentary before or after.\n";
    }

    private String tripDetailsBlock(Trip trip, TripPreference preference) {
        String interests = (preference != null && preference.getInterests() != null)
                ? preference.getInterests() : "general sightseeing";
        String additional = (preference != null && preference.getAdditionalPreferences() != null)
                ? preference.getAdditionalPreferences() : "none";

        return String.format(
                "Source: %s%nDestination: %s%nDates: %s to %s%nTravelers: %d%nBudget: %s%n"
                        + "Travel style: %s%nInterests: %s%nAdditional preferences: %s%n"
                        + "Preferred transport: %s%nAccommodation preference: %s%n",
                trip.getSource(), trip.getDestination(), trip.getStartDate(), trip.getEndDate(),
                trip.getTravelers(), trip.getBudget(),
                trip.getTravelStyle() != null ? trip.getTravelStyle() : "BALANCED",
                interests, additional,
                trip.getPreferredTransport() != null ? trip.getPreferredTransport() : "not specified",
                trip.getAccommodationPreference() != null ? trip.getAccommodationPreference() : "not specified"
        );
    }

    private String weatherBlock(Trip trip, List<PlaceSearchResult> candidatePlaces) {
        if (candidatePlaces.isEmpty()) return "";

        // Use the first candidate place's coordinates as a proxy for the
        // destination's location (see WeatherService for why this is
        // same-day-only, not a real multi-day forecast).
        PlaceSearchResult anyPlace = candidatePlaces.get(0);
        Optional<WeatherForecast> forecast = weatherService.getForecast(anyPlace.latitude(), anyPlace.longitude());

        if (forecast.isEmpty()) return "";

        WeatherForecast wf = forecast.get();
        StringBuilder sb = new StringBuilder("\nCurrent weather at the destination: ")
                .append(wf.description()).append(".\n");
        if (wf.recommendIndoor()) {
            sb.append("Weather is poor (rain/storm) - prefer indoor activities ")
              .append("(museums, restaurants, indoor markets) over outdoor ones where possible, ")
              .append("and mention the weather reason for any activity you adjusted.\n");
        }
        return sb.toString();
    }

    private String candidatePlacesBlock(List<PlaceSearchResult> candidatePlaces) {
        StringBuilder sb = new StringBuilder("\nReal places you must choose from (do not invent others):\n");
        for (PlaceSearchResult p : candidatePlaces) {
            sb.append("- ").append(p.name())
              .append(" (").append(p.category()).append(")")
              .append(p.rating() != null ? ", rating " + p.rating() : "")
              .append(" — ").append(p.address());
            if (p.openingTime() != null && p.closingTime() != null) {
                sb.append(" [hours: ").append(p.openingTime()).append("-").append(p.closingTime()).append("]");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private String responseSchemaBlock() {
        return "\nRespond with ONLY a JSON object in this exact shape:\n"
                + "{\n"
                + "  \"tripTitle\": \"string\",\n"
                + "  \"summary\": \"string\",\n"
                + "  \"days\": [\n"
                + "    {\n"
                + "      \"dayNumber\": 1,\n"
                + "      \"date\": \"YYYY-MM-DD\",\n"
                + "      \"theme\": \"short theme for the day\",\n"
                + "      \"activities\": [\n"
                + "        {\n"
                + "          \"name\": \"exact name from the candidate list\",\n"
                + "          \"category\": \"string\",\n"
                + "          \"description\": \"1-2 sentences\",\n"
                + "          \"startTime\": \"HH:mm\",\n"
                + "          \"durationMinutes\": 90,\n"
                + "          \"estimatedCost\": 500,\n"
                + "          \"latitude\": 15.492,\n"
                + "          \"longitude\": 73.773,\n"
                + "          \"address\": \"string\",\n"
                + "          \"reason\": \"why this fits the traveler\"\n"
                + "        }\n"
                + "      ]\n"
                + "    }\n"
                + "  ]\n"
                + "}\n";
    }
}
