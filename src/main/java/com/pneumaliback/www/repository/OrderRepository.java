package com.pneumaliback.www.repository;

import com.pneumaliback.www.entity.Order;
import com.pneumaliback.www.entity.User;
import com.pneumaliback.www.enums.OrderStatus;
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
public interface OrderRepository extends JpaRepository<Order, Long> {
    // === Recherche par utilisateur ===
    List<Order> findByUser(User user);
    List<Order> findByUserId(Long userId);
    Page<Order> findByUserId(Long userId, Pageable pageable);
    Page<Order> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    
    // === Recherche par statut ===
    List<Order> findByStatus(OrderStatus status);
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);
    long countByStatus(OrderStatus status);
    
    List<Order> findByStatusIn(List<OrderStatus> statuses);
    
    // === Commandes par utilisateur et statut ===
    List<Order> findByUserAndStatus(User user, OrderStatus status);
    List<Order> findByUserIdAndStatus(Long userId, OrderStatus status);
    
    // === Recherche par période ===
    @Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate")
    List<Order> findByDateRange(@Param("startDate") LocalDateTime startDate, 
                               @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT o FROM Order o WHERE o.createdAt >= :startDate")
    List<Order> findFromDate(@Param("startDate") LocalDateTime startDate);
    
    // === Recherche par promotion ===
    List<Order> findByPromotionId(Long promotionId);
    
    @Query("SELECT COUNT(o) FROM Order o WHERE o.promotion.id = :promotionId")
    long countOrdersByPromotion(@Param("promotionId") Long promotionId);
    
    // === Statistiques ===
    @Query("SELECT COUNT(o) FROM Order o WHERE o.createdAt >= :startDate AND o.status != 'CANCELED'")
    long countCompletedOrdersFrom(@Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT o.status, COUNT(o) FROM Order o GROUP BY o.status")
    List<Object[]> countOrdersByStatus();
    
    @Query("SELECT SUM(oi.quantity * oi.unitPrice) FROM Order o JOIN o.items oi WHERE o.id = :orderId")
    Optional<java.math.BigDecimal> calculateOrderTotal(@Param("orderId") Long orderId);
    
    // === Commandes à traiter ===
    @Query("SELECT o FROM Order o WHERE o.status IN ('PENDING', 'CONFIRMED') ORDER BY o.createdAt ASC")
    List<Order> findPendingOrders();
    
    @Query("SELECT o FROM Order o WHERE o.status = 'SHIPPED' AND o.updatedAt < :cutoffDate")
    List<Order> findOldShippedOrders(@Param("cutoffDate") LocalDateTime cutoffDate);
}
