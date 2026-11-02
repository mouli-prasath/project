package com.tripplanner.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "trip_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "trip_id", nullable = false, unique = true)
    private Trip trip;

    // Stored as comma-separated values for MVP simplicity, e.g. "BEACHES,FOOD,NIGHTLIFE".
    // A normalized trip_interests join table is a reasonable v2 improvement.
    @Column(name = "interests", length = 500)
    private String interests;

    @Column(name = "additional_preferences", columnDefinition = "TEXT")
    private String additionalPreferences;
}
