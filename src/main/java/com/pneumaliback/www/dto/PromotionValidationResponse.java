package com.pneumaliback.www.dto;

import java.math.BigDecimal;

public record PromotionValidationResponse(
        String code,
        BigDecimal discountAmount,
        BigDecimal subtotal,
        BigDecimal totalAfterDiscount) {
}
