package com.pneumaliback.www.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pneumaliback.www.entity.QuoteRequest;
import com.pneumaliback.www.entity.QuoteRequestItem;

@Repository
public interface QuoteRequestItemRepository extends JpaRepository<QuoteRequestItem, Long> {

    List<QuoteRequestItem> findByQuoteRequest(QuoteRequest quoteRequest);

    void deleteByQuoteRequest(QuoteRequest quoteRequest);
}
