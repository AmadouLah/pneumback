package com.pneumaliback.www.repository;

import com.pneumaliback.www.entity.Influenceur;
import com.pneumaliback.www.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface InfluenceurRepository extends JpaRepository<Influenceur, Long> {
    
    Optional<Influenceur> findByUser(User user);
    
    @Query("SELECT i FROM Influenceur i WHERE i.user.id = :userId")
    Optional<Influenceur> findByUserId(@Param("userId") Long userId);
    
    List<Influenceur> findByCommissionRateGreaterThan(BigDecimal commissionRate);
    
    List<Influenceur> findByCommissionRateLessThan(BigDecimal commissionRate);
    
    List<Influenceur> findByCommissionRateBetween(BigDecimal minRate, BigDecimal maxRate);
    
    Page<Influenceur> findAllByOrderByCommissionRateDesc(Pageable pageable);
    
    Page<Influenceur> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    @Query("SELECT i FROM Influenceur i WHERE i.user.enabled = true")
    List<Influenceur> findByActiveUser();
    
    @Query("SELECT i FROM Influenceur i WHERE i.user.enabled = true ORDER BY i.commissionRate DESC")
    List<Influenceur> findByActiveUserOrderByCommissionRateDesc();
    
    @Query("SELECT COUNT(i) FROM Influenceur i")
    Long countAllInfluenceurs();
    
    @Query("SELECT COUNT(i) FROM Influenceur i WHERE i.user.enabled = true")
    Long countActiveInfluenceurs();
    
    @Query("SELECT AVG(i.commissionRate) FROM Influenceur i")
    BigDecimal findAverageCommissionRate();
    
    @Query("SELECT MAX(i.commissionRate) FROM Influenceur i")
    BigDecimal findMaxCommissionRate();
    
    @Query("SELECT MIN(i.commissionRate) FROM Influenceur i")
    BigDecimal findMinCommissionRate();
    
    boolean existsByUser(User user);
    
    void deleteByUser(User user);
}