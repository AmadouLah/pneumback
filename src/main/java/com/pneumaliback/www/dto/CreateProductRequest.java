package com.pneumaliback.www.dto;

import com.pneumaliback.www.enums.TireSeason;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateProductRequest(
        @NotBlank(message = "Le nom est requis") @Size(max = 150, message = "Le nom ne peut pas dépasser 150 caractères") String name,

        @NotNull(message = "Le prix est requis") @DecimalMin(value = "0.01", message = "Le prix doit être supérieur à 0") BigDecimal price,

        @NotNull(message = "Le stock est requis") @Min(value = 0, message = "Le stock ne peut pas être négatif") Integer stock,

        Long brandId,

        @Size(max = 50, message = "La taille ne peut pas dépasser 50 caractères") String size,

        Long widthId,

        Long profileId,

        Long diameterId,

        TireSeason season,

        Long vehicleTypeId,

        Long tireConditionId,

        @Size(max = 255, message = "L'URL de l'image ne peut pas dépasser 255 caractères") String imageUrl,

        @Size(max = 1000, message = "La description ne peut pas dépasser 1000 caractères") String description,

        @NotNull(message = "L'ID de la catégorie est requis") Long categoryId,

        Boolean active) {
}
