package com.pneumaliback.www.dto.quote;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record QuoteAdminUpdateRequest(
        @Valid List<Item> items,
        BigDecimal discountTotal,
        BigDecimal totalQuoted,
        LocalDate validUntil,
        String adminNotes,
        String deliveryDetails) {

    public record Item(
            Long productId,
            @NotBlank String productName,
            String brand,
            Integer width,
            Integer profile,
            Integer diameter,
            @NotNull @Min(1) Integer quantity,
            @NotNull BigDecimal unitPrice) {
    }
}
