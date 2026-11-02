package com.tripplanner.service;

import com.tripplanner.dto.budget.BudgetResponse;
import com.tripplanner.entity.Activity;
import com.tripplanner.entity.ItineraryDay;
import com.tripplanner.entity.Trip;
import com.tripplanner.entity.TripBudget;
import com.tripplanner.exception.ResourceNotFoundException;
import com.tripplanner.repository.ItineraryDayRepository;
import com.tripplanner.repository.TripBudgetRepository;
import com.tripplanner.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final TripRepository tripRepository;
    private final ItineraryDayRepository itineraryDayRepository;
    private final TripBudgetRepository tripBudgetRepository;
    private final ItineraryService itineraryService;

    @Transactional
    public BudgetResponse getBudget(Long userId, Long tripId) {
        Trip trip = findOwnedTrip(userId, tripId);
        return computeAndSave(trip);
    }

    /**
     * Section 5.11: if the current itinerary exceeds budget, regenerate it
     * with cost-conscious instructions. If it's already within budget, this
     * is a no-op (returns the current numbers) - no reason to spend another
     * AI call on a trip that's already fine.
     */
    @Transactional
    public BudgetResponse optimizeBudget(Long userId, Long tripId) {
        Trip trip = findOwnedTrip(userId, tripId);
        BudgetResponse current = computeAndSave(trip);

        if (!current.exceeded()) {
            return current;
        }

        itineraryService.regenerate(userId, tripId, true);

        Trip refreshed = findOwnedTrip(userId, tripId);
        return computeAndSave(refreshed);
    }

    private BudgetResponse computeAndSave(Trip trip) {
        List<ItineraryDay> days = itineraryDayRepository.findByTripIdOrderByDayNumberAsc(trip.getId());

        BigDecimal accommodation = BigDecimal.ZERO;
        BigDecimal food = BigDecimal.ZERO;
        BigDecimal transport = BigDecimal.ZERO;
        BigDecimal activityCost = BigDecimal.ZERO;

        for (ItineraryDay day : days) {
            for (Activity activity : day.getActivities()) {
                BigDecimal cost = activity.getEstimatedCost() != null ? activity.getEstimatedCost() : BigDecimal.ZERO;
                switch (categorize(activity.getCategory())) {
                    case ACCOMMODATION -> accommodation = accommodation.add(cost);
                    case FOOD -> food = food.add(cost);
                    case TRANSPORT -> transport = transport.add(cost);
                    default -> activityCost = activityCost.add(cost);
                }
            }
        }

        BigDecimal total = accommodation.add(food).add(transport).add(activityCost)
                .setScale(2, RoundingMode.HALF_UP);

        TripBudget budgetRow = tripBudgetRepository.findByTripId(trip.getId())
                .orElseGet(() -> TripBudget.builder().trip(trip).build());

        budgetRow.setAccommodationCost(accommodation);
        budgetRow.setFoodCost(food);
        budgetRow.setTransportCost(transport);
        budgetRow.setActivityCost(activityCost);
        budgetRow.setTotalEstimatedCost(total);
        tripBudgetRepository.save(budgetRow);

        BigDecimal budget = trip.getBudget() != null ? trip.getBudget() : BigDecimal.ZERO;
        BigDecimal remaining = budget.subtract(total).setScale(2, RoundingMode.HALF_UP);

        return new BudgetResponse(
                accommodation, food, transport, activityCost, total, budget, remaining,
                remaining.compareTo(BigDecimal.ZERO) < 0
        );
    }

    private enum CostBucket { ACCOMMODATION, FOOD, TRANSPORT, ACTIVITY }

    /**
     * Buckets an AI-assigned free-text category into a budget line item.
     * Best-effort keyword matching - the AI's "category" field is
     * conversational (e.g. "HISTORY", "Food & Dining"), not a fixed enum, so
     * exact matching isn't realistic. Anything unmatched counts as a general
     * activity cost, which is the safe default.
     */
    private CostBucket categorize(String category) {
        if (category == null) return CostBucket.ACTIVITY;
        String c = category.toLowerCase(Locale.ROOT);

        if (c.contains("hotel") || c.contains("accommodation") || c.contains("stay")) {
            return CostBucket.ACCOMMODATION;
        }
        if (c.contains("food") || c.contains("restaurant") || c.contains("meal")
                || c.contains("lunch") || c.contains("dinner") || c.contains("breakfast") || c.contains("cafe")) {
            return CostBucket.FOOD;
        }
        if (c.contains("transport") || c.contains("transfer") || c.contains("taxi") || c.contains("cab")) {
            return CostBucket.TRANSPORT;
        }
        return CostBucket.ACTIVITY;
    }

    private Trip findOwnedTrip(Long userId, Long tripId) {
        return tripRepository.findByIdAndUserId(tripId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));
    }
}
