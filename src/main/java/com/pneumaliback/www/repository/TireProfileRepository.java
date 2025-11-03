package com.pneumaliback.www.repository;

import com.pneumaliback.www.entity.TireProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TireProfileRepository extends JpaRepository<TireProfile, Long> {
    List<TireProfile> findByActiveTrueOrderByValueAsc();

    Optional<TireProfile> findByValue(Integer value);

    boolean existsByValue(Integer value);
}
