package com.example.clothingstore.repository;

import com.example.clothingstore.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query("SELECT p.name, SUM(oi.quantity) FROM OrderItem oi " +
            "JOIN oi.product p " +
            "JOIN oi.order o " +
            "WHERE o.orderDate BETWEEN :start AND :end " +
            "AND o.deleted = false " +
            "GROUP BY p.id, p.name " +
            "ORDER BY SUM(oi.quantity) DESC")
    List<Object[]> findTopSellingProducts(@Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end);

    @Query("SELECT c.name, SUM(oi.quantity * oi.unitPrice) FROM OrderItem oi " +
            "JOIN oi.product p " +
            "JOIN p.category c " +
            "JOIN oi.order o " +
            "WHERE o.orderDate BETWEEN :start AND :end " +
            "AND o.deleted = false " +
            "GROUP BY c.id, c.name " +
            "ORDER BY SUM(oi.quantity * oi.unitPrice) DESC")
    List<Object[]> getRevenueByCategory(@Param("start") LocalDateTime start,
                                        @Param("end") LocalDateTime end);
}