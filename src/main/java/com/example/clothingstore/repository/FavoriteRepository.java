package com.example.clothingstore.repository;

import com.example.clothingstore.model.Favorite;
import com.example.clothingstore.model.Product;
import com.example.clothingstore.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    Optional<Favorite> findByUserAndProduct(User user, Product product);

    Optional<Favorite> findByUserIdAndProductId(Long userId, Long productId);

    List<Favorite> findByUserOrderByCreatedAtDesc(User user);


    Favorite findFirstByUserOrderByCreatedAtAsc(User user);

    @Query("SELECT f.product.id FROM Favorite f WHERE f.user = :user")
    List<Long> findProductIdsByUser(@Param("user") User user);

    boolean existsByUserAndProductId(User user, Long productId);
    @Modifying
    @Query("DELETE FROM Favorite f WHERE f.user = :user")
    void deleteByUser(@Param("user") User user);

    @Query("SELECT COUNT(f) FROM Favorite f WHERE f.user = :user")
    long countByUser(@Param("user") User user);

    @Query("SELECT f FROM Favorite f WHERE f.user = :user AND f.product.id IN :productIds")
    List<Favorite> findByUserAndProductIdIn(@Param("user") User user, @Param("productIds") List<Long> productIds);
}