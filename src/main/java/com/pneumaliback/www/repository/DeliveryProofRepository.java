package com.pneumaliback.www.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pneumaliback.www.entity.DeliveryProof;
import com.pneumaliback.www.entity.QuoteRequest;

@Repository
public interface DeliveryProofRepository extends JpaRepository<DeliveryProof, Long> {
    Optional<DeliveryProof> findByQuoteRequest(QuoteRequest quoteRequest);
}

