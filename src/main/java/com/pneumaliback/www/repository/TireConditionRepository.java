package com.pneumaliback.www.repository;

import com.pneumaliback.www.entity.TireCondition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TireConditionRepository extends JpaRepository<TireCondition, Long> {
    @Query("SELECT t FROM TireCondition t WHERE t.active = true ORDER BY t.name")
    List<TireCondition> findByActiveTrueOrderByNameAsc();

    Optional<TireCondition> findByNameIgnoreCase(String name);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.tireCondition.id = :tireConditionId")
    long countProductsByTireConditionId(@Param("tireConditionId") Long tireConditionId);
}
