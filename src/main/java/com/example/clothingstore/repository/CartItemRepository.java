package com.example.clothingstore.repository;

import com.example.clothingstore.model.CartItem;
import com.example.clothingstore.model.Product;
import com.example.clothingstore.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByUser(User user);
    void deleteByUser(User user);

    @Query("SELECT COUNT(ci) FROM CartItem ci WHERE ci.user = :user")
    int countByUser(@Param("user") User user);
    Optional<CartItem> findByUserAndProductAndSizeAndColor(User user, Product product, String size, String color);
    @Query("SELECT COUNT(DISTINCT ci.user) FROM CartItem ci WHERE ci.id IS NOT NULL AND " +
            "EXISTS (SELECT 1 FROM CartItem ci2 WHERE ci2.user = ci.user AND ci2.id IS NOT NULL)")
    Long countDistinctUsersWithCartItems(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}