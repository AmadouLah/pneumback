package com.pneumaliback.www.repository;

import com.pneumaliback.www.entity.Payment;
import com.pneumaliback.www.entity.Order;
import com.pneumaliback.www.enums.PaymentMethod;
import com.pneumaliback.www.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrder(Order order);
    Optional<Payment> findByOrderId(Long orderId);
    Optional<Payment> findByTransactionReference(String transactionReference);
    
    List<Payment> findByStatus(PaymentStatus status);
    Page<Payment> findByStatus(PaymentStatus status, Pageable pageable);
    long countByStatus(PaymentStatus status);
    
    List<Payment> findByMethod(PaymentMethod method);
    Page<Payment> findByMethod(PaymentMethod method, Pageable pageable);
    long countByMethod(PaymentMethod method);
    
    List<Payment> findByMethodAndStatus(PaymentMethod method, PaymentStatus status);
    
    // === Recherche par montant ===
    List<Payment> findByAmountBetween(BigDecimal minAmount, BigDecimal maxAmount);
    List<Payment> findByAmountGreaterThanEqual(BigDecimal minAmount);
    
    // === Recherche par période ===
    @Query("SELECT p FROM Payment p WHERE p.createdAt BETWEEN :startDate AND :endDate")
    List<Payment> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                 @Param("endDate") LocalDateTime endDate);
    
    // === Statistiques ===
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = 'SUCCESS'")
    BigDecimal getTotalSuccessfulPayments();
    
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = 'SUCCESS' AND p.createdAt >= :startDate")
    BigDecimal getTotalSuccessfulPaymentsFrom(@Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT p.method, COUNT(p), SUM(p.amount) FROM Payment p WHERE p.status = 'SUCCESS' GROUP BY p.method")
    List<Object[]> getPaymentStatsByMethod();
    
    @Query("SELECT p.status, COUNT(p) FROM Payment p GROUP BY p.status")
    List<Object[]> getPaymentStatsByStatus();
    
    // === Paiements échoués/en attente ===
    @Query("SELECT p FROM Payment p WHERE p.status = 'PENDING' AND p.createdAt < :cutoffTime")
    List<Payment> findExpiredPendingPayments(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    @Query("SELECT p FROM Payment p WHERE p.status = 'FAILED' AND p.createdAt >= :startDate")
    List<Payment> findRecentFailedPayments(@Param("startDate") LocalDateTime startDate);
}