package com.pneumaliback.www.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pneumaliback.www.entity.Commission;
import com.pneumaliback.www.entity.Order;
import com.pneumaliback.www.enums.CommissionStatus;

public interface CommissionRepository extends JpaRepository<Commission, Long> {
    Optional<Commission> findByOrder(Order order);

    List<Commission> findByInfluenceurId(Long influenceurId);

    List<Commission> findByInfluenceurIdAndStatus(Long influenceurId, CommissionStatus status);

    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM Commission c WHERE c.influenceur.id = :infId")
    java.math.BigDecimal sumByInfluenceur(@Param("infId") Long influenceurId);

    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM Commission c WHERE c.influenceur.id = :infId AND c.status = :status")
    java.math.BigDecimal sumByInfluenceurAndStatus(@Param("infId") Long influenceurId, @Param("status") CommissionStatus status);
}
