package com.tripplanner.service.hotel;

import com.tripplanner.dto.hotel.HotelCandidate;
import com.tripplanner.dto.hotel.HotelResponse;
import com.tripplanner.dto.hotel.HotelSearchResponse;
import com.tripplanner.entity.*;
import com.tripplanner.exception.ResourceNotFoundException;
import com.tripplanner.repository.ItineraryDayRepository;
import com.tripplanner.repository.TripBudgetRepository;
import com.tripplanner.repository.TripRepository;
import com.tripplanner.service.map.DistanceUtil;
import com.tripplanner.service.map.OsmMapsClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class HotelService {

    private static final String PROVIDER_NAME = "Google Places API (New) - Nearby Search";
    private static final String DISCLAIMER =
            "Hotel details are supplied live by Google Places and may not reflect real-time "
                    + "availability or exact pricing. Price level is Google's coarse category, not a "
                    + "guaranteed rate. Verify details with the hotel or a booking provider before booking.";

    // Google's price-level enum, ordered cheapest to most expensive.
    private static final List<String> PRICE_LEVEL_ORDER = List.of(
            "PRICE_LEVEL_FREE", "PRICE_LEVEL_INEXPENSIVE", "PRICE_LEVEL_MODERATE",
            "PRICE_LEVEL_EXPENSIVE", "PRICE_LEVEL_VERY_EXPENSIVE"
    );

    private final TripRepository tripRepository;
    private final ItineraryDayRepository itineraryDayRepository;
    private final TripBudgetRepository tripBudgetRepository;

    private final OsmMapsClient mapsClient;
    private final OsmHotelClient hotelClient;

    @Transactional(readOnly = true)
    public HotelSearchResponse search(
            Long userId, Long tripId,
            int radiusKm, Double minRating, Double maxDistanceKm,
            String maxPriceLevel, List<String> requestedAmenities
    ) {
        Trip trip = findOwnedTrip(userId, tripId);
        List<ItineraryDay> days = itineraryDayRepository.findByTripIdOrderByDayNumberAsc(tripId);

        double[] center = resolveCenterPoint(trip, days);

        int radiusMeters = Math.min(Math.max(radiusKm, 1), 20) * 1000;
        List<HotelCandidate> candidates = hotelClient.searchNearbyHotels(center[0], center[1], radiusMeters, 20);

        BudgetContext budgetContext = computeBudgetContext(trip);

        List<HotelResponse> results = new ArrayList<>();
        for (HotelCandidate c : candidates) {
            double distanceFromCenter = DistanceUtil.haversineKm(center[0], center[1], c.latitude(), c.longitude());

            NearestActivity nearest = findNearestActivity(days, c.latitude(), c.longitude());

            if (minRating != null && (c.rating() == null || c.rating() < minRating)) continue;
            if (maxDistanceKm != null && distanceFromCenter > maxDistanceKm) continue;
            if (maxPriceLevel != null && !passesPriceFilter(c.priceLevel(), maxPriceLevel)) continue;
            if (requestedAmenities != null && !requestedAmenities.isEmpty() && !hasAnyAmenity(c.amenities(), requestedAmenities)) continue;

            Boolean withinBudget = (budgetContext.suggestedMaxPriceLevel() != null && c.priceLevel() != null)
                    ? passesPriceFilter(c.priceLevel(), budgetContext.suggestedMaxPriceLevel())
                    : null; // unknown - OSM currently never populates priceLevel, see HotelResponse javadoc

            results.add(new HotelResponse(
                    c.placeId(), c.name(), c.address(), c.rating(), c.userRatingCount(),
                    c.priceLevel(), c.imageUrl(), c.latitude(), c.longitude(), c.amenities(),
                    c.websiteUrl() != null ? c.websiteUrl() : c.googleMapsUrl(),
                    round(distanceFromCenter),
                    nearest != null ? nearest.name() : null,
                    nearest != null ? round(nearest.distanceKm()) : null,
                    withinBudget
            ));
        }

        return new HotelSearchResponse(
                tripId, center[0], center[1], radiusKm, PROVIDER_NAME, DISCLAIMER,
                budgetContext.suggestedMaxPriceLevel(), budgetContext.remainingBudgetPerNight(),
                results
        );
    }

    /**
     * Centroid of every persisted activity's coordinates, if the itinerary
     * has been generated. Falls back to geocoding the trip's destination
     * string when no itinerary exists yet - hotel discovery still works
     * before generation, just centered on the city rather than the actual
     * planned stops.
     */
    private double[] resolveCenterPoint(Trip trip, List<ItineraryDay> days) {
        List<double[]> points = new ArrayList<>();
        for (ItineraryDay day : days) {
            for (Activity activity : day.getActivities()) {
                Double lat = activity.getPlace().getLatitude();
                Double lng = activity.getPlace().getLongitude();
                if (lat != null && lng != null) {
                    points.add(new double[]{lat, lng});
                }
            }
        }

        if (points.isEmpty()) {
            var geocoded = mapsClient.geocode(trip.getDestination());
            return new double[]{geocoded.latitude(), geocoded.longitude()};
        }

        double sumLat = 0, sumLng = 0;
        for (double[] p : points) {
            sumLat += p[0];
            sumLng += p[1];
        }
        return new double[]{sumLat / points.size(), sumLng / points.size()};
    }

    private NearestActivity findNearestActivity(List<ItineraryDay> days, double hotelLat, double hotelLng) {
        NearestActivity nearest = null;
        for (ItineraryDay day : days) {
            for (Activity activity : day.getActivities()) {
                Double lat = activity.getPlace().getLatitude();
                Double lng = activity.getPlace().getLongitude();
                if (lat == null || lng == null) continue;

                double distance = DistanceUtil.haversineKm(hotelLat, hotelLng, lat, lng);
                if (nearest == null || distance < nearest.distanceKm()) {
                    nearest = new NearestActivity(activity.getPlace().getName(), distance);
                }
            }
        }
        return nearest;
    }

    /**
     * Derives a suggested price-level ceiling and a real remaining-budget-
     * per-night figure from data the app already trusts (trip.budget and
     * the already-computed non-accommodation cost estimate from
     * BudgetService/TripBudget) - not fabricated, just arithmetic. The
     * currency-to-price-level threshold table below is our own app-defined
     * heuristic (assumes INR, matching the spec's own budget examples) for
     * turning that real number into one of Google's coarse tiers - clearly
     * NOT provider data, and documented as such.
     */
    private BudgetContext computeBudgetContext(Trip trip) {
        if (trip.getBudget() == null) {
            return new BudgetContext(null, null);
        }

        TripBudget existing = tripBudgetRepository.findByTripId(trip.getId()).orElse(null);
        BigDecimal otherCosts = BigDecimal.ZERO;
        if (existing != null) {
            if (existing.getFoodCost() != null) otherCosts = otherCosts.add(existing.getFoodCost());
            if (existing.getTransportCost() != null) otherCosts = otherCosts.add(existing.getTransportCost());
            if (existing.getActivityCost() != null) otherCosts = otherCosts.add(existing.getActivityCost());
        }

        BigDecimal remainingForAccommodation = trip.getBudget().subtract(otherCosts);
        if (remainingForAccommodation.compareTo(BigDecimal.ZERO) < 0) {
            remainingForAccommodation = BigDecimal.ZERO;
        }

        long nights = Math.max(1, ChronoUnit.DAYS.between(trip.getStartDate(), trip.getEndDate()));
        BigDecimal perNight = remainingForAccommodation
                .divide(BigDecimal.valueOf(nights), 2, RoundingMode.HALF_UP);

        String suggestedTier = suggestPriceLevel(perNight);

        return new BudgetContext(suggestedTier, perNight.doubleValue());
    }

    /** App-defined thresholds (assumes INR) - see computeBudgetContext javadoc. */
    private String suggestPriceLevel(BigDecimal perNight) {
        double value = perNight.doubleValue();
        if (value < 1500) return "PRICE_LEVEL_INEXPENSIVE";
        if (value < 5000) return "PRICE_LEVEL_MODERATE";
        if (value < 12000) return "PRICE_LEVEL_EXPENSIVE";
        return "PRICE_LEVEL_VERY_EXPENSIVE";
    }

    private boolean passesPriceFilter(String hotelPriceLevel, String maxPriceLevel) {
        if (hotelPriceLevel == null) return true; // unknown - don't exclude, benefit of the doubt
        int hotelRank = PRICE_LEVEL_ORDER.indexOf(hotelPriceLevel);
        int maxRank = PRICE_LEVEL_ORDER.indexOf(maxPriceLevel);
        if (hotelRank == -1 || maxRank == -1) return true;
        return hotelRank <= maxRank;
    }

    private boolean hasAnyAmenity(List<String> hotelAmenities, List<String> requested) {
        if (hotelAmenities == null || hotelAmenities.isEmpty()) return false;
        for (String req : requested) {
            for (String have : hotelAmenities) {
                if (have.toLowerCase(Locale.ROOT).contains(req.toLowerCase(Locale.ROOT))) return true;
            }
        }
        return false;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private Trip findOwnedTrip(Long userId, Long tripId) {
        return tripRepository.findByIdAndUserId(tripId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));
    }

    private record NearestActivity(String name, double distanceKm) {}

    private record BudgetContext(String suggestedMaxPriceLevel, Double remainingBudgetPerNight) {}
}
