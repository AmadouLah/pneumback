package com.pneumaliback.www.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.pneumaliback.www.entity.QuoteRequest;
import com.pneumaliback.www.entity.User;
import com.pneumaliback.www.enums.QuoteStatus;

@Repository
public interface QuoteRequestRepository extends JpaRepository<QuoteRequest, Long> {

    Optional<QuoteRequest> findByRequestNumber(String requestNumber);

    Optional<QuoteRequest> findByQuoteNumber(String quoteNumber);

    @EntityGraph(attributePaths = { "items", "user", "assignedLivreur" })
    List<QuoteRequest> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = { "items", "user", "assignedLivreur" })
    List<QuoteRequest> findByUserOrderByCreatedAtDesc(User user);

    @EntityGraph(attributePaths = { "items", "user", "assignedLivreur" })
    List<QuoteRequest> findByStatusInOrderByCreatedAtDesc(List<QuoteStatus> statuses);

    @EntityGraph(attributePaths = { "items", "user", "assignedLivreur" })
    List<QuoteRequest> findByAssignedLivreurOrderByCreatedAtDesc(User livreur);

    @Query("""
            select distinct qr
            from QuoteRequest qr
            left join fetch qr.items
            left join fetch qr.user
            left join fetch qr.assignedLivreur
            where qr.id = :id
            """)
    Optional<QuoteRequest> findDetailedById(@Param("id") Long id);

    @EntityGraph(attributePaths = { "items", "user", "assignedLivreur" })
    @Query("""
            select distinct qr
            from QuoteRequest qr
            where qr.quotePdfUrl is not null and qr.quotePdfUrl != ''
            order by qr.createdAt desc
            """)
    List<QuoteRequest> findAllWithPdfUrl();
}
