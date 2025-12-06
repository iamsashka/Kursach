package com.example.clothingstore.repository;

import com.example.clothingstore.model.Category;
import com.example.clothingstore.model.Product;
import com.example.clothingstore.model.ProductTag;
import com.example.clothingstore.model.TargetAudience;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findTop4ByCategoryAndIdNot(Category category, Long id);
    Page<Product> findAllByDeletedFalse(Pageable pageable);
    Page<Product> findByNameContainingIgnoreCaseAndDeletedFalse(String name, Pageable pageable);
    Page<Product> findByCategoryIdAndDeletedFalse(Long categoryId, Pageable pageable);
    Page<Product> findByBrandIdAndDeletedFalse(Long brandId, Pageable pageable);
    List<Product> findByDeletedFalse();

    @Query("SELECT p FROM Product p WHERE p.deleted = false " +
            "AND (:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))) " +
            "AND (:categoryId IS NULL OR p.category.id = :categoryId) " +
            "AND (:brandId IS NULL OR p.brand.id = :brandId) " +
            "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
            "AND (:maxPrice IS NULL OR p.price <= :maxPrice) " +
            "AND (:size IS NULL OR p.sizes LIKE CONCAT('%', :size, '%')) " +
            "AND (:country IS NULL OR LOWER(p.countryOfOrigin) LIKE LOWER(CONCAT('%', :country, '%'))) " +
            "AND (:audience IS NULL OR p.targetAudience = :audience) " +
            "AND (:tag IS NULL OR :tag MEMBER OF p.tags) " +
            "AND (:color IS NULL OR EXISTS (SELECT 1 FROM p.colors c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :color, '%'))))")
    Page<Product> findByFiltersAlternative(@Param("name") String name,
                                           @Param("categoryId") Long categoryId,
                                           @Param("brandId") Long brandId,
                                           @Param("minPrice") BigDecimal minPrice,
                                           @Param("maxPrice") BigDecimal maxPrice,
                                           @Param("color") String color,
                                           @Param("size") String size,
                                           @Param("country") String country,
                                           @Param("audience") TargetAudience audience,
                                           @Param("tag") ProductTag tag,
                                           Pageable pageable);

    Page<Product> findByTagsContainingAndDeletedFalse(ProductTag tag, Pageable pageable);
    Page<Product> findByTagsContainingAndDeletedFalseOrderByCreatedAtDesc(ProductTag tag, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.deleted = false AND :tag MEMBER OF p.tags ORDER BY p.createdAt DESC")
    List<Product> findByTagOrderByCreatedAtDesc(@Param("tag") ProductTag tag, Pageable pageable);

    Page<Product> findByTargetAudienceAndDeletedFalse(TargetAudience audience, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.deleted = false AND p.originalPrice IS NOT NULL AND p.originalPrice > p.price")
    Page<Product> findDiscountedProducts(Pageable pageable);

    @Query("SELECT DISTINCT c.name FROM Product p JOIN p.colors c WHERE p.deleted = false ORDER BY c.name")
    List<String> findDistinctColors();

    @Query("SELECT DISTINCT p.countryOfOrigin FROM Product p WHERE p.deleted = false AND p.countryOfOrigin IS NOT NULL ORDER BY p.countryOfOrigin")
    List<String> findDistinctCountries();

    @Query(value = """
    SELECT DISTINCT TRIM(UNNEST(STRING_TO_ARRAY(p.sizes, ','))) as size 
    FROM products p 
    WHERE p.deleted = false AND p.sizes IS NOT NULL AND p.sizes != '' 
    ORDER BY size
    """, nativeQuery = true)
    List<String> findDistinctSizes();

    @Query("SELECT p FROM Product p WHERE p.deleted = false AND p.originalPrice IS NOT NULL " +
            "AND ((p.originalPrice - p.price) / p.originalPrice * 100) BETWEEN :minDiscount AND :maxDiscount")
    Page<Product> findByDiscountRange(@Param("minDiscount") BigDecimal minDiscount,
                                      @Param("maxDiscount") BigDecimal maxDiscount,
                                      Pageable pageable);

    Page<Product> findByBrandIdAndCategoryIdAndDeletedFalse(Long brandId, Long categoryId, Pageable pageable);

    Page<Product> findByStockQuantityGreaterThanAndDeletedFalse(Integer minStock, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.deleted = false ORDER BY p.views DESC, p.rating DESC")
    Page<Product> findPopularProducts(Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.deleted = false AND " +
            "EXISTS (SELECT 1 FROM p.tags t WHERE t IN :tags)")
    Page<Product> findByMultipleTags(@Param("tags") List<ProductTag> tags, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.deleted = false AND p.createdAt BETWEEN :startDate AND :endDate")
    Page<Product> findByCreatedAtBetween(@Param("startDate") java.time.LocalDateTime startDate,
                                         @Param("endDate") java.time.LocalDateTime endDate,
                                         Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.deleted = false AND p.originalPrice IS NOT NULL " +
            "ORDER BY (p.originalPrice - p.price) DESC")
    Page<Product> findTopDiscountedProducts(Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.deleted = false AND p.rating >= :minRating")
    Page<Product> findByRatingGreaterThanEqual(@Param("minRating") Double minRating, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.deleted = false AND " +
            "EXISTS (SELECT 1 FROM p.colors c WHERE LOWER(c.name) = LOWER(:colorName))")
    Page<Product> findByColorName(@Param("colorName") String colorName, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.deleted = false AND " +
            "EXISTS (SELECT 1 FROM p.colors c WHERE c.name IN :colorNames)")
    Page<Product> findByColorNames(@Param("colorNames") List<String> colorNames, Pageable pageable);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.deleted = false")
    Long countActiveProducts();

    @Query("SELECT COUNT(p) FROM Product p WHERE p.deleted = false AND p.tags IS NOT EMPTY")
    Long countProductsWithTags();

    @Query("SELECT COUNT(p) FROM Product p WHERE p.deleted = false AND p.originalPrice IS NOT NULL AND p.originalPrice > p.price")
    Long countDiscountedProducts();

    @Query("SELECT p FROM Product p WHERE p.deleted = false ORDER BY p.views DESC")
    Page<Product> findMostViewedProducts(Pageable pageable);

    @Query("SELECT p.name FROM Product p WHERE p.deleted = false AND LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<String> findProductNamesContaining(@Param("query") String query, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.id IN :ids AND p.deleted = false")
    List<Product> findByIdInAndDeletedFalse(@Param("ids") List<Long> ids);
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdIgnoreDeleted(@Param("id") Long id);
    @Modifying
    @Query("UPDATE Product p SET p.stockQuantity = p.stockQuantity - :quantity WHERE p.id = :id AND p.deleted = false")
    void decreaseStockQuantity(@Param("id") Long id, @Param("quantity") Integer quantity);
    Page<Product> findByDeletedTrue(Pageable pageable);
    List<Product> findByDeletedTrue();

    long countByDeletedFalse();
    long countByDeletedTrue();

    @Query("SELECT p FROM Product p WHERE " +
            "(:search IS NULL OR LOWER(CAST(p.name AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))) AND " +
            "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
            "(:brandId IS NULL OR p.brand.id = :brandId) AND " +
            "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
            "(:maxPrice IS NULL OR p.price <= :maxPrice) AND " +
            "p.deleted = false")
    Page<Product> findByFiltersForExport(@Param("search") String search,
                                         @Param("categoryId") Long categoryId,
                                         @Param("brandId") Long brandId,
                                         @Param("minPrice") BigDecimal minPrice,
                                         @Param("maxPrice") BigDecimal maxPrice,
                                         Pageable pageable);
    Page<Product> findByDeletedFalse(Pageable pageable);
    List<Product> findByNameContainingIgnoreCaseAndDeletedFalse(String name);
    @Query("SELECT p FROM Product p WHERE " +
            "(:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
            "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
            "(:brandId IS NULL OR p.brand.id = :brandId) AND " +
            "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
            "(:maxPrice IS NULL OR p.price <= :maxPrice) AND " +
            "p.deleted = false")
    List<Product> findBySimpleFilters(@Param("search") String search,
                                      @Param("categoryId") Long categoryId,
                                      @Param("brandId") Long brandId,
                                      @Param("minPrice") BigDecimal minPrice,
                                      @Param("maxPrice") BigDecimal maxPrice);
    @Query("SELECT p FROM Product p WHERE " +
            "p.deleted = false AND " +
            "(:search IS NULL OR LOWER(CAST(p.name AS text)) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%'))) AND " +
            "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
            "(:brandId IS NULL OR p.brand.id = :brandId) AND " +
            "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
            "(:maxPrice IS NULL OR p.price <= :maxPrice) AND " +
            "(:color IS NULL OR EXISTS (SELECT 1 FROM p.colors c WHERE LOWER(CAST(c.name AS text)) LIKE LOWER(CONCAT('%', CAST(:color AS text), '%')))) AND " +
            "(:size IS NULL OR p.sizes LIKE CONCAT('%', CAST(:size AS text), '%')) AND " +
            "(:country IS NULL OR LOWER(CAST(p.countryOfOrigin AS text)) LIKE LOWER(CONCAT('%', CAST(:country AS text), '%'))) AND " +
            "(:audience IS NULL OR p.targetAudience = :audience) AND " +
            "(:tag IS NULL OR :tag MEMBER OF p.tags)")
    Page<Product> findByFilters(@Param("search") String search,
                                @Param("categoryId") Long categoryId,
                                @Param("brandId") Long brandId,
                                @Param("minPrice") BigDecimal minPrice,
                                @Param("maxPrice") BigDecimal maxPrice,
                                @Param("color") String color,
                                @Param("size") String size,
                                @Param("country") String country,
                                @Param("audience") TargetAudience audience,
                                @Param("tag") ProductTag tag,
                                Pageable pageable);

}