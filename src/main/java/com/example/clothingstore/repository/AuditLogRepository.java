package com.example.clothingstore.repository;

import com.example.clothingstore.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByEntityTypeOrderByCreatedAtDesc(String entityType, Pageable pageable);

    Page<AuditLog> findByActionOrderByCreatedAtDesc(String action, Pageable pageable);

    Page<AuditLog> findByUsernameOrderByCreatedAtDesc(String username, Pageable pageable);

    List<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, Long entityId);

    Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE a.createdAt BETWEEN :startDate AND :endDate ORDER BY a.createdAt DESC")
    Page<AuditLog> findByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    @Query("SELECT DISTINCT a.entityType FROM AuditLog a")
    List<String> findDistinctEntityTypes();

    @Query("SELECT DISTINCT a.action FROM AuditLog a")
    List<String> findDistinctActions();
    @Query("SELECT a FROM AuditLog a WHERE " +
            "(:entityType IS NULL OR a.entityType = :entityType) AND " +
            "(:action IS NULL OR a.action = :action) AND " +
            "(:username IS NULL OR a.username LIKE %:username%) " +
            "ORDER BY a.createdAt DESC")
    Page<AuditLog> findWithFilters(@Param("entityType") String entityType,
                                   @Param("action") String action,
                                   @Param("username") String username,
                                   Pageable pageable);

}