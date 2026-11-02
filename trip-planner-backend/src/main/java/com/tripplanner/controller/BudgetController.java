package com.tripplanner.controller;

import com.tripplanner.dto.budget.BudgetResponse;
import com.tripplanner.security.UserPrincipal;
import com.tripplanner.service.BudgetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/trips/{tripId}/budget")
@RequiredArgsConstructor
@Tag(name = "Budget", description = "Trip cost estimate and AI-driven budget optimization")
public class BudgetController {

    private final BudgetService budgetService;

    @GetMapping
    public ResponseEntity<BudgetResponse> getBudget(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long tripId
    ) {
        return ResponseEntity.ok(budgetService.getBudget(principal.getId(), tripId));
    }

    @PostMapping("/optimize")
    public ResponseEntity<BudgetResponse> optimizeBudget(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long tripId
    ) {
        return ResponseEntity.ok(budgetService.optimizeBudget(principal.getId(), tripId));
    }
}
