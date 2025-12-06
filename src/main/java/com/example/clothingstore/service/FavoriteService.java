package com.example.clothingstore.service;

import com.example.clothingstore.model.Favorite;
import com.example.clothingstore.model.Product;
import com.example.clothingstore.model.User;
import com.example.clothingstore.repository.FavoriteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;

    @Transactional
    public Favorite addToFavorites(User user, Product product) {
        Optional<Favorite> existingFavorite = favoriteRepository.findByUserAndProduct(user, product);

        if (existingFavorite.isPresent()) {
            throw new RuntimeException("Товар уже в избранном");
        }

        Favorite favorite = new Favorite(user, product);
        return favoriteRepository.save(favorite);
    }

    @Transactional
    public void removeFromFavorites(User user, Long productId) {
        Favorite favorite = favoriteRepository.findByUserIdAndProductId(user.getId(), productId)
                .orElseThrow(() -> new RuntimeException("Товар не найден в избранном"));

        favoriteRepository.delete(favorite);
    }

    public boolean isProductInFavorites(User user, Long productId) {
        return favoriteRepository.existsByUserAndProductId(user, productId);
    }

    public long getUserFavoritesCount(User user) {
        return favoriteRepository.countByUser(user);
    }

    public Favorite getOldestFavorite(User user) {
        return favoriteRepository.findFirstByUserOrderByCreatedAtAsc(user);
    }

    public void removeFavoriteById(Long favoriteId) {
        favoriteRepository.deleteById(favoriteId);
    }

    public List<Favorite> getUserFavorites(User user) {
        return favoriteRepository.findByUserOrderByCreatedAtDesc(user);
    }
    public Map<Long, Boolean> getFavoriteStatusForProducts(User user, List<Long> productIds) {
        Map<Long, Boolean> result = new HashMap<>();
        List<Favorite> favorites = favoriteRepository.findByUserAndProductIdIn(user, productIds);
        Set<Long> favoriteProductIds = favorites.stream()
                .map(f -> f.getProduct().getId())
                .collect(Collectors.toSet());

        for (Long productId : productIds) {
            result.put(productId, favoriteProductIds.contains(productId));
        }

        return result;
    }
    @Transactional
    public void clearUserFavorites(User user) {
        favoriteRepository.deleteByUser(user);
    }
}