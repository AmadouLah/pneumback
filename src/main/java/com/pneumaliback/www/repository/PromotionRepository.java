package com.pneumaliback.www.repository;

import com.pneumaliback.www.entity.Promotion;
import com.pneumaliback.www.enums.PromotionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    
    Optional<Promotion> findByCode(String code);
    
    List<Promotion> findByType(PromotionType type);
    
    @Query("SELECT p FROM Promotion p WHERE p.startDate <= :currentDate AND p.endDate >= :currentDate")
    List<Promotion> findActivePromotions(@Param("currentDate") LocalDate currentDate);
    
    @Query("SELECT p FROM Promotion p WHERE p.startDate <= :currentDate AND p.endDate >= :currentDate")
    Page<Promotion> findActivePromotions(@Param("currentDate") LocalDate currentDate, Pageable pageable);
    
    @Query("SELECT p FROM Promotion p WHERE p.endDate < :currentDate")
    List<Promotion> findExpiredPromotions(@Param("currentDate") LocalDate currentDate);
    
    @Query("SELECT p FROM Promotion p WHERE p.startDate > :currentDate")
    List<Promotion> findFuturePromotions(@Param("currentDate") LocalDate currentDate);
    
    List<Promotion> findByDiscountPercentageGreaterThan(BigDecimal percentage);
    
    List<Promotion> findByDiscountPercentageBetween(BigDecimal minPercentage, BigDecimal maxPercentage);
    
    List<Promotion> findByStartDateBetween(LocalDate startDate, LocalDate endDate);
    
    List<Promotion> findByEndDateBetween(LocalDate startDate, LocalDate endDate);
    
    @Query("SELECT p FROM Promotion p WHERE p.code = :code AND p.startDate <= :currentDate AND p.endDate >= :currentDate")
    Optional<Promotion> findActivePromotionByCode(@Param("code") String code, @Param("currentDate") LocalDate currentDate);
    
    @Query("SELECT p FROM Promotion p WHERE p.type = :type AND p.startDate <= :currentDate AND p.endDate >= :currentDate")
    List<Promotion> findActivePromotionsByType(@Param("type") PromotionType type, @Param("currentDate") LocalDate currentDate);
    
    @Query("SELECT COUNT(o) FROM Promotion p JOIN p.orders o WHERE p.id = :promotionId")
    Long countOrdersByPromotionId(@Param("promotionId") Long promotionId);
    
    @Query("SELECT p FROM Promotion p ORDER BY p.discountPercentage DESC")
    List<Promotion> findAllOrderByDiscountPercentageDesc();
    
    @Query("SELECT p FROM Promotion p WHERE p.endDate BETWEEN :startDate AND :endDate")
    List<Promotion> findPromotionsExpiringBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query("SELECT p FROM Promotion p WHERE p.type = :type ORDER BY p.discountPercentage DESC")
    List<Promotion> findByTypeOrderByDiscountPercentageDesc(@Param("type") PromotionType type);
    
    @Query("SELECT COUNT(p) FROM Promotion p WHERE p.startDate <= :currentDate AND p.endDate >= :currentDate")
    Long countActivePromotions(@Param("currentDate") LocalDate currentDate);
    
    @Query("SELECT AVG(p.discountPercentage) FROM Promotion p WHERE p.startDate <= :currentDate AND p.endDate >= :currentDate")
    BigDecimal findAverageActiveDiscountPercentage(@Param("currentDate") LocalDate currentDate);
    
    boolean existsByCode(String code);
    
    boolean existsByCodeAndIdNot(String code, Long id);
}