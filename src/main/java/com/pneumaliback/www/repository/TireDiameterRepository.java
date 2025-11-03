package com.pneumaliback.www.repository;

import com.pneumaliback.www.entity.TireDiameter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TireDiameterRepository extends JpaRepository<TireDiameter, Long> {
    List<TireDiameter> findByActiveTrueOrderByValueAsc();

    Optional<TireDiameter> findByValue(Integer value);

    boolean existsByValue(Integer value);
}
