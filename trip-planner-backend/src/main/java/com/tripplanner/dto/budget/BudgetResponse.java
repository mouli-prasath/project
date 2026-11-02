package com.tripplanner.dto.budget;

import java.math.BigDecimal;

public record BudgetResponse(
        BigDecimal accommodationCost,
        BigDecimal foodCost,
        BigDecimal transportCost,
        BigDecimal activityCost,
        BigDecimal totalEstimatedCost,
        BigDecimal budget,
        BigDecimal remaining,      // budget - totalEstimatedCost (negative if exceeded)
        boolean exceeded
) {}
