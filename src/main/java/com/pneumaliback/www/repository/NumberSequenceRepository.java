package com.pneumaliback.www.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pneumaliback.www.entity.NumberSequence;

@Repository
public interface NumberSequenceRepository extends JpaRepository<NumberSequence, Long> {

    Optional<NumberSequence> findBySequenceKeyAndYear(String sequenceKey, int year);
}
