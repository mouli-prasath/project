package com.tripplanner.dto.trip;

import org.springframework.data.domain.Page;

import java.util.List;

// Generic pagination wrapper, per spec's pagination requirement
// (GET /api/trips?page=0&size=10).
public record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {
    public static <T> PagedResponse<T> from(Page<T> page) {
        return new PagedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
