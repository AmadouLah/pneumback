package com.pneumaliback.www.dto;

import com.pneumaliback.www.enums.PromotionType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PromotionResponse(
                Long id,
                String code,
                PromotionType type,
                BigDecimal discountPercentage,
                BigDecimal discountAmount,
                LocalDate startDate,
                LocalDate endDate,
                boolean active,
                InfluenceurInfo influenceur) {
        public record InfluenceurInfo(
                        Long id,
                        String firstName,
                        String lastName,
                        String email) {
        }
}
