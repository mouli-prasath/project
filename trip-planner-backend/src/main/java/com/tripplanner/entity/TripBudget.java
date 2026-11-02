package com.tripplanner.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;

@Entity
@Table(name = "trip_budget")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripBudget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "trip_id", nullable = false, unique = true)
    private Trip trip;

    @Column(name = "accommodation_cost", precision = 12, scale = 2)
    private BigDecimal accommodationCost;

    @Column(name = "food_cost", precision = 12, scale = 2)
    private BigDecimal foodCost;

    @Column(name = "transport_cost", precision = 12, scale = 2)
    private BigDecimal transportCost;

    @Column(name = "activity_cost", precision = 12, scale = 2)
    private BigDecimal activityCost;

    @Column(name = "total_estimated_cost", precision = 12, scale = 2)
    private BigDecimal totalEstimatedCost;
}
