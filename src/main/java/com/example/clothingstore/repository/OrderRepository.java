package com.example.clothingstore.repository;

import com.example.clothingstore.model.Order;
import com.example.clothingstore.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findAllByDeletedFalse(Pageable pageable);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.user.id = :userId AND o.deleted = false")
    long countByUserIdAndDeletedFalse(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.user.id = :userId AND o.deleted = false")
    BigDecimal getTotalSpentByUserId(@Param("userId") Long userId);

    @Query("SELECT o FROM Order o WHERE o.user.id = :userId AND o.deleted = false ORDER BY o.orderDate DESC LIMIT 5")
    List<Order> findRecentOrdersByUserId(@Param("userId") Long userId);

    @Query("SELECT o FROM Order o WHERE o.deleted = false AND " +
            "(:userName IS NULL OR LOWER(o.user.firstName) LIKE LOWER(CONCAT('%', :userName, '%')) OR " +
            "LOWER(o.user.lastName) LIKE LOWER(CONCAT('%', :userName, '%')) OR " +
            "LOWER(o.user.email) LIKE LOWER(CONCAT('%', :userName, '%')))")
    Page<Order> findByUserNameContaining(@Param("userName") String userName, Pageable pageable);

    Page<Order> findByUserIdAndDeletedFalseOrderByOrderDateDesc(Long userId, Pageable pageable);
    Page<Order> findByUserIdAndDeletedFalse(Long userId, Pageable pageable);

    Long countByOrderDateBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.orderDate BETWEEN :start AND :end")
    BigDecimal getTotalRevenueByPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT c.name, COUNT(o) FROM Order o JOIN o.products p JOIN p.category c " +
            "WHERE o.orderDate BETWEEN :start AND :end GROUP BY c.name")
    List<Object[]> getOrderCountByCategory(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT c.name, SUM(o.totalAmount) FROM Order o JOIN o.products p JOIN p.category c " +
            "WHERE o.orderDate BETWEEN :start AND :end GROUP BY c.name")
    List<Object[]> getRevenueByCategory(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT p.name, COUNT(o) FROM Order o JOIN o.products p " +
            "WHERE o.orderDate BETWEEN :start AND :end GROUP BY p.name ORDER BY COUNT(o) DESC LIMIT :limit")
    List<Object[]> getTopSellingProducts(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end,
                                         @Param("limit") int limit);

    @Query("SELECT COUNT(DISTINCT o.user) FROM Order o WHERE o.orderDate BETWEEN :start AND :end")
    Long countDistinctUsersWithOrders(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT CAST(o.orderDate AS LocalDate), COUNT(o) FROM Order o " +
            "WHERE CAST(o.orderDate AS LocalDate) BETWEEN :start AND :end " +
            "GROUP BY CAST(o.orderDate AS LocalDate) ORDER BY CAST(o.orderDate AS LocalDate)")
    List<Object[]> getDailyOrderCount(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT CAST(o.orderDate AS LocalDate), SUM(o.totalAmount) FROM Order o " +
            "WHERE CAST(o.orderDate AS LocalDate) BETWEEN :start AND :end " +
            "GROUP BY CAST(o.orderDate AS LocalDate) ORDER BY CAST(o.orderDate AS LocalDate)")
    List<Object[]> getDailyRevenue(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT p.name, COUNT(o), p.id FROM Order o JOIN o.products p " +
            "WHERE o.orderDate BETWEEN :start AND :end GROUP BY p.name, p.id ORDER BY COUNT(o) DESC LIMIT :limit")
    List<Object[]> getTopSellingProductsWithIds(@Param("start") LocalDateTime start,
                                                @Param("end") LocalDateTime end,
                                                @Param("limit") int limit);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.orderDate BETWEEN :start AND :end " +
            "AND o.status IN :statuses AND o.deleted = false")
    Long countByOrderDateBetweenAndStatusIn(@Param("start") LocalDateTime start,
                                            @Param("end") LocalDateTime end,
                                            @Param("statuses") List<OrderStatus> statuses);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o " +
            "WHERE o.orderDate BETWEEN :start AND :end " +
            "AND o.status IN :statuses AND o.deleted = false")
    BigDecimal calculateRevenueByStatusIn(@Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end,
                                          @Param("statuses") List<OrderStatus> statuses);

    @Query("SELECT CAST(o.orderDate AS LocalDate), COUNT(o) FROM Order o " +
            "WHERE o.orderDate BETWEEN :start AND :end " +
            "AND o.deleted = false " +
            "GROUP BY CAST(o.orderDate AS LocalDate) " +
            "ORDER BY CAST(o.orderDate AS LocalDate)")
    List<Object[]> getDailyOrderCounts(@Param("start") LocalDateTime start,
                                       @Param("end") LocalDateTime end);

    Page<Order> findByStatusAndDeletedFalse(OrderStatus status, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.deleted = false AND " +
            "(LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(o.user.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(o.user.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(o.user.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(o.shippingAddress) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Order> searchOrders(@Param("search") String search, Pageable pageable);
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.user LEFT JOIN FETCH o.products WHERE o.status = :status")
    Page<Order> findByStatusWithDetails(@Param("status") OrderStatus status, Pageable pageable);
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.user LEFT JOIN FETCH o.products WHERE o.id = :id")
    Optional<Order> findByIdWithDetails(@Param("id") Long id);

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);
    Page<Order> findByDeletedTrue(Pageable pageable);
    long countByStatus(OrderStatus status);

    Page<Order> findByDeletedFalse(Pageable pageable);
    @Query("SELECT o FROM Order o WHERE o.deleted = false AND " +
            "(o.orderNumber LIKE %:search% OR o.user.email LIKE %:search% OR o.shippingAddress LIKE %:search%)")
    Page<Order> searchActiveOrders(@Param("search") String search, Pageable pageable);
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.deleted = false")
    BigDecimal sumTotalAmountOfAllOrders();

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.user LEFT JOIN FETCH o.products WHERE o.deleted = false")
    List<Order> findAllWithDetails();

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.user LEFT JOIN FETCH o.products WHERE o.deleted = false AND " +
            "(o.orderNumber LIKE %:search% OR o.user.firstName LIKE %:search% OR o.user.lastName LIKE %:search% OR o.user.email LIKE %:search% OR o.shippingAddress LIKE %:search%)")
    List<Order> searchOrdersWithDetails(@Param("search") String search);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.user LEFT JOIN FETCH o.products WHERE o.deleted = false AND o.status = :status")
    List<Order> findByStatusWithDetails(@Param("status") OrderStatus status);

}