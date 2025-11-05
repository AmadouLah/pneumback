package com.pneumaliback.www.dto;

import com.pneumaliback.www.enums.PromotionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PromotionCreateDTO(
                @NotBlank(message = "Le code promo est obligatoire") @Size(max = 100, message = "Le code promo ne peut pas dépasser 100 caractères") String code,

                @NotNull(message = "Le type de promotion est obligatoire") PromotionType type,

                BigDecimal discountPercentage,

                @DecimalMin(value = "0.01", message = "Le montant fixe doit être supérieur à 0") BigDecimal discountAmount,

                @NotNull(message = "La date de début est obligatoire") LocalDate startDate,

                @NotNull(message = "La date de fin est obligatoire") LocalDate endDate,

                @NotNull(message = "L'influenceur est obligatoire") Long influenceurId) {
}
