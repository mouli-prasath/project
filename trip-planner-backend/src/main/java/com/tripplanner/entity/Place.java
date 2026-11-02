package com.tripplanner.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Entity
@Table(name = "places")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Place {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 50)
    private String category;

    // No precision/scale here deliberately: those annotations are for
    // fixed-point types (BigDecimal -> DECIMAL/NUMERIC columns). Double maps
    // to a native floating-point SQL column, which doesn't take
    // precision/scale the same way - applying them caused a schema
    // generation error on startup. A plain Double column has more than
    // enough precision for lat/lng anyway.
    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(length = 500)
    private String address;

    // Added per spec's "Opening Hours" requirement, used later by the
    // itinerary generator/validator to avoid scheduling closed locations.
    @Column(name = "opening_time")
    private LocalTime openingTime;

    @Column(name = "closing_time")
    private LocalTime closingTime;
}
