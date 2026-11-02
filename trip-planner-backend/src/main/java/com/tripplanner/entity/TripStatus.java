package com.tripplanner.entity;

public enum TripStatus {
    DRAFT,       // created but itinerary not yet generated
    GENERATED,   // AI itinerary successfully generated
    FAILED,      // generation attempted and failed
    COMPLETED    // trip end_date is in the past (can be computed or set by a scheduled job later)
}
