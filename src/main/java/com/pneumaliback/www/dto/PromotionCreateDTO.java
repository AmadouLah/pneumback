package com.pneumaliback.www.dto;

import com.pneumaliback.www.enums.PromotionType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PromotionCreateDTO(
        String code,
        PromotionType type,
        BigDecimal discountPercentage,
        BigDecimal discountAmount,
        LocalDate startDate,
        LocalDate endDate,
        Long influenceurId
) {}
