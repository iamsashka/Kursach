package com.example.clothingstore.repository;

import com.example.clothingstore.model.Role;
import com.example.clothingstore.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmailAndDeletedFalse(String email);

    boolean existsByEmailAndDeletedFalse(String email);
    boolean existsByUsernameAndDeletedFalse(String username);

    Page<User> findAllByDeletedFalse(Pageable pageable);
    Page<User> findByRolesContainingAndDeletedFalse(Role role, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.deleted = true OR u.enabled = false")
    Page<User> findByDeletedTrueOrEnabledFalse(Pageable pageable);
    List<User> findByEnabledTrueAndDeletedFalse();

    @Query("SELECT u FROM User u WHERE u.deleted = false AND " +
            "(LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<User> searchUsers(@Param("search") String search, Pageable pageable);


    @Query("SELECT u FROM User u WHERE u.deleted = false AND :role MEMBER OF u.roles AND " +
            "(LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<User> searchCustomers(@Param("search") String search, @Param("role") Role role, Pageable pageable);

    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt BETWEEN :start AND :end AND u.deleted = false")
    Long countByCreatedAtBetween(@Param("start") LocalDateTime start,
                                 @Param("end") LocalDateTime end);

    @Query("SELECT u.city, COUNT(u) FROM User u WHERE u.createdAt BETWEEN :start AND :end AND u.city IS NOT NULL GROUP BY u.city")
    List<Object[]> getUsersByCity(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    @Query("SELECT u FROM User u WHERE u.deleted = false AND NOT EXISTS " +
            "(SELECT r FROM u.roles r WHERE r = com.example.clothingstore.model.Role.ROLE_ADMIN)")
    Page<User> findAllByDeletedFalseAndNotAdmin(Pageable pageable);
    default Page<User> searchCustomers(String search, Pageable pageable) {
        return searchCustomers(search, Role.ROLE_CUSTOMER, pageable);
    }

    long countByEnabledTrueAndDeletedFalse();
    long countByRolesContainingAndDeletedFalse(Role role);
    long countByRolesContainingAndEnabledTrueAndDeletedFalse(Role role);
    Page<User> findByDeletedFalseAndEnabledTrue(Pageable pageable);
    Page<User> findByDeletedTrue(Pageable pageable);
    @Query("SELECT u FROM User u WHERE u.deleted = false AND u.enabled = true AND " +
            "(LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<User> searchActiveCustomers(@Param("search") String search, Pageable pageable);
    @Query("SELECT u FROM User u WHERE u.deleted = false AND u.lastActivity >= :cutoffDate")
    Page<User> findRecentlyActiveCustomers(@Param("cutoffDate") LocalDateTime cutoffDate, Pageable pageable);
    @Query("SELECT u FROM User u WHERE u.deleted = true AND " +
            "(LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<User> searchDeletedCustomers(@Param("search") String search, Pageable pageable);
}