package com.tripplanner.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "destinations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Destination {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 100)
    private String country;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Free-text category used for filtering, e.g. BEACH, MOUNTAIN, HISTORY,
    // ADVENTURE, CITY, WILDLIFE. Kept as a simple string rather than an enum
    // since new destination categories shouldn't require a code change.
    @Column(length = 30)
    private String category;

    private Double latitude;
    private Double longitude;

    // Simple static ranking for "Popular destinations". A real product would
    // compute this from trip-creation counts; a fixed seed value is a
    // reasonable placeholder that's honest about being one (see
    // ExploreService/README - not derived from real usage data yet).
    @Column(name = "popularity_score")
    @Builder.Default
    private Integer popularityScore = 0;
}
