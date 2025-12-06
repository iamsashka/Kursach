package com.example.clothingstore.repository;

import com.example.clothingstore.model.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByDeletedFalse();
    Optional<Category> findByNameAndDeletedFalse(String name);

    boolean existsByNameAndDeletedFalse(String name);

    @Query("SELECT c FROM Category c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) AND c.deleted = false")
    List<Category> findByNameContainingIgnoreCaseAndDeletedFalse(@Param("query") String query);

    Page<Category> findByDeletedFalse(Pageable pageable);
    Page<Category> findByDeletedTrue(Pageable pageable);

    @Query("SELECT c FROM Category c WHERE " +
            "LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(c.description) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Category> searchCategories(@Param("search") String search, Pageable pageable);

    List<Category> findByDeletedTrue();

    @Query("SELECT c FROM Category c WHERE " +
            "LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(c.description) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<Category> searchCategories(@Param("search") String search);

    long countByDeletedFalse();
    long countByDeletedTrue();

}