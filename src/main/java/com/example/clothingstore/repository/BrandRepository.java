package com.example.clothingstore.repository;

import com.example.clothingstore.model.Brand;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

public interface BrandRepository extends JpaRepository<Brand, Long> {

    // Найти все неудаленные бренды
    List<Brand> findByDeletedFalse();

    // Проверить существование бренда по имени (только неудаленные)
    boolean existsByNameAndDeletedFalse(String name);

    // Найти бренд по имени (только неудаленные)
    Optional<Brand> findByNameAndDeletedFalse(String name);

    // Поиск брендов по названию (без учета регистра) среди неудаленных
    @Query("SELECT b FROM Brand b WHERE LOWER(b.name) LIKE LOWER(CONCAT('%', :query, '%')) AND b.deleted = false")
    List<Brand> findByNameContainingIgnoreCaseAndDeletedFalse(@Param("query") String query);

    @Query("SELECT COUNT(b) FROM Brand b")
    long countTotalBrands();

    @Query("SELECT COUNT(b) FROM Brand b WHERE b.deleted = false")
    long countActiveBrands();

    @Query("SELECT COUNT(b) FROM Brand b WHERE b.deleted = true")
    long countArchivedBrands();

    Page<Brand> findByDeletedTrue(Pageable pageable);
    // Для админки - найти все бренды включая удаленные
    @Query("SELECT b FROM Brand b")
    List<Brand> findAllIncludingDeleted();
    // Пагинация
    Page<Brand> findByDeletedFalse(Pageable pageable);

    @Query("SELECT b FROM Brand b WHERE " +
            "LOWER(b.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(b.contactEmail) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Brand> searchBrands(@Param("search") String search, Pageable pageable);
}