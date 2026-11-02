package com.tripplanner.dto.favorite;

import com.tripplanner.entity.Favorite;

import java.time.LocalDateTime;

public record FavoriteResponse(
        Long id,
        Long placeId,
        String placeName,
        String address,
        String category,
        Double latitude,
        Double longitude,
        LocalDateTime createdAt
) {
    public static FavoriteResponse from(Favorite favorite) {
        return new FavoriteResponse(
                favorite.getId(),
                favorite.getPlace().getId(),
                favorite.getPlace().getName(),
                favorite.getPlace().getAddress(),
                favorite.getPlace().getCategory(),
                favorite.getPlace().getLatitude(),
                favorite.getPlace().getLongitude(),
                favorite.getCreatedAt()
        );
    }
}
