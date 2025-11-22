package com.pneumaliback.www.dto.quote;

import java.math.BigDecimal;

public record QuoteItemResponse(
        Long id,
        Long productId,
        String productName,
        String brand,
        Integer width,
        Integer profile,
        Integer diameter,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal) {
}
