package com.pneumaliback.www.repository;

import com.pneumaliback.www.entity.TireWidth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TireWidthRepository extends JpaRepository<TireWidth, Long> {
    List<TireWidth> findByActiveTrueOrderByValueAsc();

    Optional<TireWidth> findByValue(Integer value);

    boolean existsByValue(Integer value);
}
