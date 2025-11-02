package com.pneumaliback.www.dto;

import com.pneumaliback.www.enums.TireSeason;
import com.pneumaliback.www.enums.VehicleType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateProductRequest(
        @Size(max = 150, message = "Le nom ne peut pas dépasser 150 caractères") String name,

        @DecimalMin(value = "0.01", message = "Le prix doit être supérieur à 0") BigDecimal price,

        @Min(value = 0, message = "Le stock ne peut pas être négatif") Integer stock,

        @Size(max = 50, message = "La marque ne peut pas dépasser 50 caractères") String brand,

        @Size(max = 50, message = "La taille ne peut pas dépasser 50 caractères") String size,

        TireSeason season,

        VehicleType vehicleType,

        @Size(max = 255, message = "L'URL de l'image ne peut pas dépasser 255 caractères") String imageUrl,

        @Size(max = 1000, message = "La description ne peut pas dépasser 1000 caractères") String description,

        Long categoryId,

        Boolean active) {
}
