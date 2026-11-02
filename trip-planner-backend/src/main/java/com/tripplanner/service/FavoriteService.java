package com.tripplanner.service;

import com.tripplanner.dto.favorite.CreateFavoriteRequest;
import com.tripplanner.dto.favorite.FavoriteResponse;
import com.tripplanner.dto.trip.PagedResponse;
import com.tripplanner.entity.Favorite;
import com.tripplanner.entity.Place;
import com.tripplanner.entity.User;
import com.tripplanner.exception.DuplicateResourceException;
import com.tripplanner.exception.ResourceNotFoundException;
import com.tripplanner.repository.FavoriteRepository;
import com.tripplanner.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final PlaceService placeService;

    @Transactional
    public FavoriteResponse addFavorite(Long userId, CreateFavoriteRequest request) {
        Place place = placeService.findOrCreate(
                request.placeName(), request.description(), request.category(),
                request.latitude(), request.longitude(), request.address(), null, null);

        if (favoriteRepository.existsByUserIdAndPlaceId(userId, place.getId())) {
            throw new DuplicateResourceException("This place is already in your favorites");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Favorite favorite = Favorite.builder().user(user).place(place).build();
        return FavoriteResponse.from(favoriteRepository.save(favorite));
    }

    @Transactional(readOnly = true)
    public PagedResponse<FavoriteResponse> listFavorites(Long userId, Pageable pageable) {
        Page<FavoriteResponse> page = favoriteRepository.findByUserId(userId, pageable)
                .map(FavoriteResponse::from);
        return PagedResponse.from(page);
    }

    @Transactional
    public void removeFavorite(Long userId, Long favoriteId) {
        Favorite favorite = favoriteRepository.findById(favoriteId)
                .orElseThrow(() -> new ResourceNotFoundException("Favorite not found"));

        if (!favorite.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Favorite not found");
        }

        favoriteRepository.delete(favorite);
    }
}
